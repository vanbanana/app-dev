package com.example.sketchto3view.data.queue

import com.example.sketchto3view.domain.model.GenerationTask
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Represents a task waiting in the queue with its current position.
 */
data class QueuedTask(
    val task: GenerationTask,
    val position: Int
)

/**
 * Manages concurrent execution of generation tasks with FIFO ordering.
 *
 * Uses a Semaphore to limit concurrent executions to [maxConcurrent] tasks,
 * a ConcurrentLinkedQueue for FIFO pending task ordering, and a configurable
 * dispatch delay between consecutive task executions to prevent API rate limiting.
 */
class TaskQueue(
    private val maxConcurrent: Int = 3,
    private val dispatchDelayMs: Long = 1000L,
    private val coroutineScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) {

    private val semaphore = Semaphore(maxConcurrent)
    private val pendingTasks = ConcurrentLinkedQueue<PendingEntry>()

    private val _activeTasks = MutableStateFlow<Set<String>>(emptySet())
    /** Set of task IDs currently being executed. */
    val activeTasks: StateFlow<Set<String>> = _activeTasks.asStateFlow()

    private val _activeTaskCount = MutableStateFlow(0)
    /** Number of tasks currently being executed. */
    val activeTaskCount: StateFlow<Int> = _activeTaskCount.asStateFlow()

    private val _queueState = MutableStateFlow<List<QueuedTask>>(emptyList())
    /** Observable list of pending tasks with their queue positions for UI observation. */
    val queueState: StateFlow<List<QueuedTask>> = _queueState.asStateFlow()

    /**
     * Internal entry holding the task and its executor callback.
     */
    private data class PendingEntry(
        val task: GenerationTask,
        val executor: suspend (GenerationTask) -> Unit
    )

    /**
     * Enqueues a task for execution. The task will be added to the pending queue
     * and dispatched when a semaphore permit becomes available.
     *
     * Execution flow:
     * 1. Task is added to the pending queue (FIFO)
     * 2. Queue state is updated for UI observation
     * 3. A coroutine is launched to acquire the semaphore
     * 4. Once acquired, the task is moved from pending to active
     * 5. A dispatch delay is applied before execution
     * 6. The executor is invoked with the task
     * 7. On completion (success or failure), the semaphore is released
     *
     * @param task The generation task to enqueue
     * @param executor The suspend function that performs the actual task execution
     */
    fun enqueue(task: GenerationTask, executor: suspend (GenerationTask) -> Unit) {
        val entry = PendingEntry(task, executor)
        pendingTasks.add(entry)
        updateQueueState()

        coroutineScope.launch {
            semaphore.acquire()
            try {
                // Remove from pending queue and update state
                pendingTasks.removeAll { it.task.id == task.id }
                addToActive(task.id)
                updateQueueState()

                // Apply dispatch delay for rate limiting
                delay(dispatchDelayMs)

                // Execute the task
                executor(task)
            } finally {
                removeFromActive(task.id)
                semaphore.release()
            }
        }
    }

    /**
     * Returns the 1-based queue position of a task in the pending queue.
     *
     * @param taskId The ID of the task to look up
     * @return The 1-based position if the task is in the pending queue, null otherwise
     */
    fun getQueuePosition(taskId: String): Int? {
        val index = pendingTasks.toList().indexOfFirst { it.task.id == taskId }
        return if (index >= 0) index + 1 else null
    }

    /**
     * Cancels a pending task by removing it from the queue.
     * Tasks that are already being executed cannot be cancelled through this method.
     *
     * @param taskId The ID of the task to cancel
     * @return true if the task was found and removed, false otherwise
     */
    fun cancelTask(taskId: String): Boolean {
        val removed = pendingTasks.removeAll { it.task.id == taskId }
        if (removed) {
            updateQueueState()
        }
        return removed
    }

    /**
     * Checks whether a task is currently being executed.
     */
    fun isActive(taskId: String): Boolean {
        return _activeTasks.value.contains(taskId)
    }

    /**
     * Checks whether a task is in the pending queue.
     */
    fun isPending(taskId: String): Boolean {
        return pendingTasks.any { it.task.id == taskId }
    }

    private fun addToActive(taskId: String) {
        _activeTasks.value = _activeTasks.value + taskId
        _activeTaskCount.value = _activeTasks.value.size
    }

    private fun removeFromActive(taskId: String) {
        _activeTasks.value = _activeTasks.value - taskId
        _activeTaskCount.value = _activeTasks.value.size
    }

    private fun updateQueueState() {
        _queueState.value = pendingTasks.toList().mapIndexed { index, entry ->
            QueuedTask(
                task = entry.task,
                position = index + 1
            )
        }
    }
}
