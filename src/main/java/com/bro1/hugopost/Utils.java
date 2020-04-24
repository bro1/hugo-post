package com.bro1.hugopost;

import java.net.URL;

import javafx.fxml.FXMLLoader;
import javafx.fxml.JavaFXBuilderFactory;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;


public class Utils {
	
	public static void open(String resourceName, String title, StandardControllerInterface controller, Stage parentStageForModal) {
		Stage stage = new Stage();

		URL res = controller.getClass().getResource(resourceName);
        System.out.println(res);
		FXMLLoader loader = new FXMLLoader();

		loader.setLocation(res);
		loader.setBuilderFactory(new JavaFXBuilderFactory());

		controller.setStage(stage);
		loader.setController(controller);

		Parent root = null;
		
		try {
			root = (Parent) loader.load(res.openStream());
		} catch (Exception e) {			
            e.printStackTrace(System.out);
		}
		
		Scene scene = new Scene(root);
		
		stage.setScene(scene);

		stage.setTitle(title);
                
                if (parentStageForModal != null) {
                    stage.initModality(Modality.WINDOW_MODAL);
                    stage.initOwner(parentStageForModal);
                }
                
		stage.show();

	}
	
}
