// Three-view generation prompts, ported verbatim from the original Sketch3View
// Android app (DefaultImageGenerationApi.kt). Each prompt produces a single
// horizontal image containing Front / Side / Top orthographic views on a pure
// white background, optimized for downstream 3D reconstruction.

export type PromptStyle = "realistic" | "chibi";

export const THREE_VIEW_PROMPT_REALISTIC = `Scene:
Pure white background (#FFFFFF), no environment, no ground plane, no cast shadows.
Soft neutral studio lighting from upper-left to clearly define form and volume.

Subject:
Three orthographic projection views of the FULL BODY character/object from the input image, arranged HORIZONTALLY in a single row: Front View (left), Side View (center), Top View (right).
Transform the sketch/drawing into a REALISTIC 3D-model-ready reference with clear volume.

CRITICAL - FULL BODY REQUIREMENT:
Even if the input shows only a head or bust, generate a FULL BODY character/object in all three views.
Extrapolate the full body design based on the visible parts of the input image.
ALL views must show the complete figure from head to toe.

Important details:
- Each view occupies EXACTLY one-third of the total image width with EQUAL white gaps between them.
- REALISTIC proportions with clear volumetric form - show depth through subtle shading.
- Clean, sharp silhouette edges - this is critical for 3D AI reconstruction.
- Soft ambient occlusion to define where surfaces meet.
- Consistent neutral gray material appearance (like a clay/maquette render).
- NO heavy textures - keep surfaces smooth to emphasize form over detail.
- Each view at the SAME scale, perfectly aligned on the same baseline.
- Emphasize the overall 3D VOLUME and SILHOUETTE over surface details.
- Think of this as a sculptor's reference: form first, detail second.

Use case:
3D modeling reference sheet for AI-powered 3D reconstruction (Tripo, Meshy, etc).

Constraints:
- NO background elements, NO ground shadows, NO perspective distortion.
- NO excessive surface detail - prioritize clean silhouette and volume.
- NO labels, NO text, NO annotations, NO dimensions, NO watermark.
- Do NOT add busy textures or patterns that obscure the form.
- Do NOT deviate from the equal-thirds horizontal layout.
- Keep lighting CONSISTENT across all three views.
- Output image MUST be in 16:9 landscape aspect ratio (wider than tall).`;

export const THREE_VIEW_PROMPT_CHIBI = `IMPORTANT: COMPLETELY TRANSFORM the input into chibi style. Do NOT preserve the realistic style of the input image. IGNORE the art style, rendering, and proportions of the input. Only use the input as a CHARACTER DESIGN REFERENCE for identity/outfit.

Scene:
Pure white background (#FFFFFF), no environment, no ground plane, no shadows.
Flat even lighting, no dramatic shadows.

Subject:
Three orthographic views of a CHIBI/Q-version (cute stylized) FULL BODY interpretation of the character from the input image, arranged HORIZONTALLY: Front (left), Side (center), Top (right).
COMPLETELY TRANSFORM into adorable chibi proportions. This is NOT a realistic rendering.

CRITICAL - STYLE OVERRIDE:
The output MUST look like a Nendoroid/Pop Mart figure, NOT a realistic rendering.
Even if the input is photorealistic or semi-realistic, the output MUST be 100% chibi cartoon style.
Exaggerate the head to be 2-3x the body size. Make limbs short and stubby.
This is a COMPLETE STYLE TRANSFORMATION, not a slight modification.

CRITICAL - FULL BODY REQUIREMENT:
Even if the input shows only a head or bust, generate a FULL BODY character in all three views.
Extrapolate the full body design based on the visible parts of the input image.
ALL views must show the complete chibi figure from head to toe.

Important details:
- Each view occupies EXACTLY one-third of the total image width with EQUAL white gaps.
- CHIBI proportions: head is 2-3x body size, tiny rounded body, stubby limbs, big round eyes.
- Smooth, clean surfaces with NO texture detail - like a vinyl toy or clay figure.
- Bold, clear silhouette outline - easily readable from any angle.
- Flat cel-shading style with minimal gradients (2-3 tone maximum).
- Round, soft edges everywhere - no sharp corners.
- Same scale and baseline alignment across all three views.
- Think: Nendoroid figure / Pop Mart blind box / vinyl designer toy.

Use case:
Cute 3D character model reference for 3D printing or figure production.

Constraints:
- NO background, NO ground, NO shadows on background, NO watermark.
- NO realistic proportions - must be CHIBI/deformed cute style.
- NO semi-realistic style - must be FULLY CARTOONIZED chibi.
- NO complex textures or patterns - keep surfaces SMOOTH and SIMPLE.
- NO text, NO labels, NO annotations.
- Do NOT preserve the input image's art style or rendering technique.
- Do NOT deviate from equal-thirds horizontal layout.
- Maximum simplicity for clean 3D printability.
- Output image MUST be in 16:9 landscape aspect ratio (wider than tall).`;

export const STYLE_PROMPTS: Record<PromptStyle, string> = {
  realistic: THREE_VIEW_PROMPT_REALISTIC,
  chibi: THREE_VIEW_PROMPT_CHIBI,
};

export const STYLE_LABELS: Record<PromptStyle, string> = {
  realistic: "写实三视图",
  chibi: "Q版三视图",
};

// When the user types a free-form prompt we still wrap it so the model keeps
// producing the three-view reference sheet layout the app is built around.
export function buildPrompt(style: PromptStyle, userPrompt?: string): string {
  const base = STYLE_PROMPTS[style];
  const trimmed = userPrompt?.trim();
  if (!trimmed) return base;
  return `${base}\n\nAdditional creative direction from the user (apply while keeping the three-view layout and constraints above):\n${trimmed}`;
}
