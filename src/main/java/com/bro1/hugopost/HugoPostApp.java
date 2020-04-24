package com.bro1.hugopost;

import java.net.URL;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.fxml.JavaFXBuilderFactory;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class HugoPostApp extends Application {

	public static Stage myStage = null;

	public static void main(String[] args) {
		Application.launch(HugoPostApp.class, args);
	}

	@Override
	public void start(Stage stage) throws Exception {

		myStage = stage;

		URL res = getClass().getResource("HugoPostHomePage.fxml");
		FXMLLoader loader = new FXMLLoader();

		loader.setLocation(res);
		loader.setBuilderFactory(new JavaFXBuilderFactory());

		HugoPostHomePageController controller = new HugoPostHomePageController();
		controller.myStage = stage;
		loader.setController(controller);

		Parent root = (Parent) loader.load(res.openStream());
		
		Scene scene = new Scene(root);
		
		stage.setScene(scene);

		stage.setTitle("Hugo Post");
		stage.show();
		
	}
}
