package com.example.hellofx;

import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class HelloJavaFX extends Application {

    @Override
    public void start(Stage stage) {
        // 2. Change "Welcome to JavaFX!" to "Welcome, <your name>!"
        String originalMessage = "Welcome, Solochi!";
        Label message = new Label(originalMessage);

        // 3. Change the button text from "Click Me" to "Start"
        Button startButton = new Button("Start");
        startButton.setOnAction(event ->
                message.setText("Great! You started the application.")
        );

        // 4. Add a second button called "Reset" that changes the label back
        Button resetButton = new Button("Reset");
        resetButton.setOnAction(event ->
                message.setText(originalMessage)
        );

        // Arrange elements vertically
        VBox layout = new VBox(15);
        layout.setAlignment(Pos.CENTER);
        layout.getChildren().addAll(message, startButton, resetButton);

        Scene scene = new Scene(layout, 500, 300);

        // 1. Change the window title to include your student number
        stage.setTitle("My First JavaFX Application - 202509615");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}