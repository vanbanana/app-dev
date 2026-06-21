package com.example.sketchto3view.data.api

import com.example.sketchto3view.domain.model.GenerationRequest
import com.example.sketchto3view.domain.model.GenerationResult
import com.example.sketchto3view.domain.service.ImageGenerationApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Default implementation of [ImageGenerationApi] using OkHttp.
 *
 * Sends a multipart request containing the source image and a prompt to the configured
 * API endpoint. The API is expected to accept:
 * - "image" part: the source image bytes (PNG)
 * - "prompt" part: the text prompt for generation
 *
 * And return:
 * - On success (2xx): the generated image bytes in the response body
 * - On error (non-2xx): an error response that is mapped to [GenerationResult.Error]
 */
@Singleton
class DefaultImageGenerationApi @Inject constructor(
    private val config: ApiLayerConfig,
    private val httpClient: OkHttpClient
) : ImageGenerationApi {

    companion object {
        /**
         * Prompt style options for the user to choose from.
         */
        enum class PromptStyle {
            REALISTIC,  // 写实风格
            CHIBI       // Q版/卡通风格
        }

        /**
         * Builds the appropriate prompt based on user's style selection.
         * All prompts are optimized for:
         * 1. GPT Image 2.0 structured format (Scene/Subject/Details/Constraints)
         * 2. Equal-thirds layout for reliable auto-cropping
         * 3. 3D model generation readiness (clear silhouette, volume emphasis, no background)
         */
        fun buildPrompt(style: PromptStyle): String {
            return when (style) {
                PromptStyle.REALISTIC -> THREE_VIEW_PROMPT_REALISTIC
                PromptStyle.CHIBI -> THREE_VIEW_PROMPT_CHIBI
            }
        }

        /**
         * 写实风格 - Realistic style optimized for 3D model generation.
         *
         * Key principles from Tripo3D best practices:
         * - Clear silhouette is more valuable than intricate internal details
         * - Volume and depth emphasis over flat detail
         * - Clean edges for AI 3D reconstruction
         * - Consistent lighting to help AI infer depth/normals
         */
        const val THREE_VIEW_PROMPT_REALISTIC =
            "Scene:\n" +
                "Pure white background (#FFFFFF), no environment, no ground plane, no cast shadows.\n" +
                "Soft neutral studio lighting from upper-left to clearly define form and volume.\n\n" +
                "Subject:\n" +
                "Three orthographic projection views of the FULL BODY character/object from the input image, " +
                "arranged HORIZONTALLY in a single row: Front View (left), Side View (center), Top View (right).\n" +
                "Transform the sketch/drawing into a REALISTIC 3D-model-ready reference with clear volume.\n\n" +
                "CRITICAL - FULL BODY REQUIREMENT:\n" +
                "Even if the input shows only a head or bust, generate a FULL BODY character/object in all three views.\n" +
                "Extrapolate the full body design based on the visible parts of the input image.\n" +
                "ALL views must show the complete figure from head to toe.\n\n" +
                "Important details:\n" +
                "- Each view occupies EXACTLY one-third of the total image width with EQUAL white gaps between them.\n" +
                "- REALISTIC proportions with clear volumetric form - show depth through subtle shading.\n" +
                "- Clean, sharp silhouette edges - this is critical for 3D AI reconstruction.\n" +
                "- Soft ambient occlusion to define where surfaces meet.\n" +
                "- Consistent neutral gray material appearance (like a clay/maquette render).\n" +
                "- NO heavy textures - keep surfaces smooth to emphasize form over detail.\n" +
                "- Each view at the SAME scale, perfectly aligned on the same baseline.\n" +
                "- Emphasize the overall 3D VOLUME and SILHOUETTE over surface details.\n" +
                "- Think of this as a sculptor's reference: form first, detail second.\n\n" +
                "Use case:\n" +
                "3D modeling reference sheet for AI-powered 3D reconstruction (Tripo, Meshy, etc).\n\n" +
                "Constraints:\n" +
                "- NO background elements, NO ground shadows, NO perspective distortion.\n" +
                "- NO excessive surface detail - prioritize clean silhouette and volume.\n" +
                "- NO labels, NO text, NO annotations, NO dimensions, NO watermark.\n" +
                "- Do NOT add busy textures or patterns that obscure the form.\n" +
                "- Do NOT deviate from the equal-thirds horizontal layout.\n" +
                "- Keep lighting CONSISTENT across all three views.\n" +
                "- Output image MUST be in 16:9 landscape aspect ratio (wider than tall)."

        /**
         * Default prompt - used when no style is specified.
         */
        const val THREE_VIEW_PROMPT = THREE_VIEW_PROMPT_REALISTIC

        /**
         * Q版/卡通风格 - Chibi/stylized style for cute 3D model generation.
         *
         * Optimized for:
         * - Exaggerated proportions (big head, small body)
         * - Clean cartoon silhouette
         * - Smooth surfaces ideal for 3D printing
         * - Clear form readable from any angle
         *
         * IMPORTANT: This prompt aggressively overrides the input image style
         * because GPT Image 2's img2img mode strongly follows the input style.
         */
        const val THREE_VIEW_PROMPT_CHIBI =
            "IMPORTANT: COMPLETELY TRANSFORM the input into chibi style. Do NOT preserve the realistic style of the input image. " +
                "IGNORE the art style, rendering, and proportions of the input. Only use the input as a CHARACTER DESIGN REFERENCE for identity/outfit.\n\n" +
                "Scene:\n" +
                "Pure white background (#FFFFFF), no environment, no ground plane, no shadows.\n" +
                "Flat even lighting, no dramatic shadows.\n\n" +
                "Subject:\n" +
                "Three orthographic views of a CHIBI/Q-version (cute stylized) FULL BODY interpretation of the character " +
                "from the input image, arranged HORIZONTALLY: Front (left), Side (center), Top (right).\n" +
                "COMPLETELY TRANSFORM into adorable chibi proportions. This is NOT a realistic rendering.\n\n" +
                "CRITICAL - STYLE OVERRIDE:\n" +
                "The output MUST look like a Nendoroid/Pop Mart figure, NOT a realistic rendering.\n" +
                "Even if the input is photorealistic or semi-realistic, the output MUST be 100% chibi cartoon style.\n" +
                "Exaggerate the head to be 2-3x the body size. Make limbs short and stubby.\n" +
                "This is a COMPLETE STYLE TRANSFORMATION, not a slight modification.\n\n" +
                "CRITICAL - FULL BODY REQUIREMENT:\n" +
                "Even if the input shows only a head or bust, generate a FULL BODY character in all three views.\n" +
                "Extrapolate the full body design based on the visible parts of the input image.\n" +
                "ALL views must show the complete chibi figure from head to toe.\n\n" +
                "Important details:\n" +
                "- Each view occupies EXACTLY one-third of the total image width with EQUAL white gaps.\n" +
                "- CHIBI proportions: head is 2-3x body size, tiny rounded body, stubby limbs, big round eyes.\n" +
                "- Smooth, clean surfaces with NO texture detail - like a vinyl toy or clay figure.\n" +
                "- Bold, clear silhouette outline - easily readable from any angle.\n" +
                "- Flat cel-shading style with minimal gradients (2-3 tone maximum).\n" +
                "- Round, soft edges everywhere - no sharp corners.\n" +
                "- Same scale and baseline alignment across all three views.\n" +
                "- Think: Nendoroid figure / Pop Mart blind box / vinyl designer toy.\n\n" +
                "Use case:\n" +
                "Cute 3D character model reference for 3D printing or figure production.\n\n" +
                "Constraints:\n" +
                "- NO background, NO ground, NO shadows on background, NO watermark.\n" +
                "- NO realistic proportions - must be CHIBI/deformed cute style.\n" +
                "- NO semi-realistic style - must be FULLY CARTOONIZED chibi.\n" +
                "- NO complex textures or patterns - keep surfaces SMOOTH and SIMPLE.\n" +
                "- NO text, NO labels, NO annotations.\n" +
                "- Do NOT preserve the input image's art style or rendering technique.\n" +
                "- Do NOT deviate from equal-thirds horizontal layout.\n" +
                "- Maximum simplicity for clean 3D printability.\n" +
                "- Output image MUST be in 16:9 landscape aspect ratio (wider than tall)."

        private const val ENDPOINT_PATH = "/v1/images/edits"
        private val MEDIA_TYPE_PNG = "image/png".toMediaType()
        private val MEDIA_TYPE_OCTET = "application/octet-stream".toMediaType()
    }

    override suspend fun generateThreeView(request: GenerationRequest): GenerationResult {
        return withContext(Dispatchers.IO) {
            try {
                val response = executeRequest(request)
                parseResponse(response)
            } catch (e: IOException) {
                GenerationResult.Error(
                    code = -1,
                    message = "Network error: ${e.message ?: "Unknown network failure"}"
                )
            } catch (e: Exception) {
                GenerationResult.Error(
                    code = -1,
                    message = "Unexpected error: ${e.message ?: "Unknown error"}"
                )
            }
        }
    }

    /**
     * Builds and executes the multipart HTTP request.
     */
    private suspend fun executeRequest(request: GenerationRequest): Response {
        val url = buildUrl()

        val multipartBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "image",
                "source_image.png",
                request.sourceImage.toRequestBody(MEDIA_TYPE_PNG)
            )
            .addFormDataPart("prompt", request.prompt)
            .addFormDataPart("size", "1792x1024") // 16:9 landscape - optimal for 3 views side by side
            .addFormDataPart("model", "gpt-image-2")
            .addFormDataPart("n", "1")
            .build()

        val httpRequest = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer ${config.apiKey}")
            .header("Accept", "image/png, application/json")
            .post(multipartBody)
            .build()

        return suspendCancellableCoroutine { continuation ->
            val call = httpClient.newCall(httpRequest)

            continuation.invokeOnCancellation {
                call.cancel()
            }

            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (continuation.isActive) {
                        continuation.resumeWithException(e)
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    if (continuation.isActive) {
                        continuation.resume(response)
                    }
                }
            })
        }
    }

    /**
     * Parses the HTTP response into a [GenerationResult].
     *
     * The API returns JSON with format: {"data": [{"url": "..."} or {"b64_json": "..."}]}
     * - For b64_json: decode and return image bytes directly as Success
     * - For url: return SuccessUrl so the caller can save the URL first (background resilience),
     *   then download separately.
     *
     * IMPORTANT: We read the response body string and close the response FIRST,
     * then perform any processing OUTSIDE the response lifecycle.
     */
    private fun parseResponse(response: Response): GenerationResult {
        // Read body and status code, then close the response immediately
        val code = response.code
        val isSuccessful = response.isSuccessful
        val bodyString: String
        try {
            bodyString = response.body?.string() ?: ""
        } finally {
            response.close()
        }

        if (!isSuccessful) {
            return GenerationResult.Error(
                code = code,
                message = bodyString.ifEmpty { "No error details" }
            )
        }

        if (bodyString.isEmpty()) {
            return GenerationResult.Error(
                code = code,
                message = "Empty response body"
            )
        }

        // Parse JSON OUTSIDE the response lifecycle
        return try {
            val jsonObj = org.json.JSONObject(bodyString)
            val dataArray = jsonObj.optJSONArray("data")
            if (dataArray != null && dataArray.length() > 0) {
                val firstItem = dataArray.getJSONObject(0)
                when {
                    firstItem.has("b64_json") -> {
                        val b64 = firstItem.getString("b64_json")
                        val imageBytes = android.util.Base64.decode(b64, android.util.Base64.DEFAULT)
                        GenerationResult.Success(imageData = imageBytes)
                    }
                    firstItem.has("url") -> {
                        val imageUrl = firstItem.getString("url")
                        // Return the URL for the caller to handle download with resilience
                        GenerationResult.SuccessUrl(imageUrl = imageUrl)
                    }
                    else -> {
                        GenerationResult.Error(
                            code = code,
                            message = "No image data in response item: ${firstItem.toString().take(200)}"
                        )
                    }
                }
            } else {
                GenerationResult.Error(
                    code = code,
                    message = "API response missing 'data' array: ${bodyString.take(200)}"
                )
            }
        } catch (e: org.json.JSONException) {
            GenerationResult.Error(
                code = code,
                message = "Failed to parse API response as JSON: ${e.message}"
            )
        } catch (e: Exception) {
            GenerationResult.Error(
                code = code,
                message = "Error processing API response: ${e.message}"
            )
        }
    }

    /**
     * Downloads an image from a URL and returns the bytes.
     * Includes retry logic (3 attempts) with exponential backoff for reliability
     * when the app is in the background.
     */
    override suspend fun downloadImageFromUrl(url: String): ByteArray? = withContext(Dispatchers.IO) {
        downloadImage(url)
    }

    /**
     * Internal image download with retry logic.
     */
    private fun downloadImage(url: String): ByteArray? {
        val maxRetries = 3
        var lastException: Exception? = null

        // Use a dedicated client with longer timeouts for image download
        val downloadClient = httpClient.newBuilder()
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .build()

        for (attempt in 1..maxRetries) {
            try {
                val request = Request.Builder()
                    .url(url)
                    .header("Accept", "image/*")
                    .build()
                val response = downloadClient.newCall(request).execute()
                if (response.isSuccessful) {
                    val bytes = response.use { it.body?.bytes() }
                    if (bytes != null && bytes.isNotEmpty()) {
                        return bytes
                    }
                } else {
                    response.close()
                }
            } catch (e: Exception) {
                lastException = e
            }

            // Wait before retry (exponential backoff: 2s, 4s, 8s)
            if (attempt < maxRetries) {
                try {
                    Thread.sleep((2000L * attempt))
                } catch (_: InterruptedException) {
                    break
                }
            }
        }

        return null
    }

    /**
     * Constructs the full API URL from the base URL and endpoint path.
     */
    private fun buildUrl(): String {
        val base = config.baseUrl.trimEnd('/')
        return "$base$ENDPOINT_PATH"
    }
}
