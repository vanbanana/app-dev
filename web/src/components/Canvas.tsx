"use client";

import { useCallback } from "react";
import { Tldraw, type Editor, type TLComponents } from "tldraw";
import "tldraw/tldraw.css";
import { ImageGenShapeUtil } from "./shapes/ImageGenShapeUtil";
import { createImageGenNode } from "./canvas-actions";
import { TopBar } from "./TopBar";
import { Toolbar } from "./Toolbar";
import { ZoomControl } from "./ZoomControl";

const customShapeUtils = [ImageGenShapeUtil];

function TextureBackground() {
  return <div className="canvas-texture" />;
}

const components: TLComponents = {
  Background: TextureBackground,
};

export default function Canvas() {
  const handleMount = useCallback((editor: Editor) => {
    editor.user.updateUserPreferences({ colorScheme: "dark" });
    editor.updateInstanceState({ isGridMode: true });

    if (editor.getCurrentPageShapeIds().size === 0) {
      const id = createImageGenNode(editor);
      editor.select(id);
      editor.zoomToFit({ animation: { duration: 0 } });
      editor.resetZoom();
      editor.centerOnPoint(editor.getShapePageBounds(id)!.center);
    }
  }, []);

  return (
    <div style={{ position: "fixed", inset: 0 }}>
      <Tldraw
        hideUi
        shapeUtils={customShapeUtils}
        components={components}
        onMount={handleMount}
      >
        <TopBar />
        <Toolbar />
        <ZoomControl />
      </Tldraw>
    </div>
  );
}
