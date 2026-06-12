package com.cubeapp.ui.games;

import com.cubeapp.model.Cube;
import com.cubeapp.ui.CubeRenderer;
import javafx.scene.Node;

import java.util.function.Consumer;

/**
 * Base class for game UI controllers.
 */
public abstract class GameController {

    protected CubeRenderer   renderer;
    protected Consumer<Cube> onCubeUpdated;
    protected Cube           cube;

    public void init(CubeRenderer renderer, Consumer<Cube> onCubeUpdated) {
        this.renderer      = renderer;
        this.onCubeUpdated = onCubeUpdated;
        this.cube          = new Cube();
        onInit();
    }

    /** Called after init() — subclasses do their setup here */
    protected abstract void onInit();

    /** Returns the root node of this controller's view */
    public abstract Node getView();

    protected void renderAndNotify() {
        renderer.render(cube);
        onCubeUpdated.accept(cube);
    }
}