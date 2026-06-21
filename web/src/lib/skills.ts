import type { PromptStyle } from "./prompts";

/** A generation "skill" — a reusable, modular generation flow the user can pick. */
export type SkillId = "general" | "realistic" | "chibi";

export interface Skill {
  id: SkillId;
  label: string;
  desc: string;
  /** three-view skills wrap the prompt as an ortho reference sheet + auto-crop */
  threeView: boolean;
  /** prompt style used by three-view skills */
  promptStyle?: PromptStyle;
}

export const SKILLS: Skill[] = [
  { id: "general", label: "通用生图", desc: "按提示词自由生成图像", threeView: false },
  {
    id: "realistic",
    label: "写实三视图",
    desc: "正 / 侧 / 顶 写实参考图",
    threeView: true,
    promptStyle: "realistic",
  },
  {
    id: "chibi",
    label: "Q版三视图",
    desc: "正 / 侧 / 顶 Q版参考图",
    threeView: true,
    promptStyle: "chibi",
  },
];

const BY_ID: Record<SkillId, Skill> = Object.fromEntries(
  SKILLS.map((s) => [s.id, s]),
) as Record<SkillId, Skill>;

export function getSkill(id: SkillId | undefined): Skill {
  return (id && BY_ID[id]) || BY_ID.general;
}

export function isThreeView(id: SkillId | undefined): boolean {
  return getSkill(id).threeView;
}
