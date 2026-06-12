package com.cubeapp;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/com/cubeapp/main.fxml")
        );
        Scene scene = new Scene(loader.load(), 1000, 620);
        stage.setTitle("CubeApp");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) { launch(args); }
}