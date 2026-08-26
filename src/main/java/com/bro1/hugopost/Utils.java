package com.bro1.hugopost;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.function.Consumer;

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

    public static String readHeader(BufferedReader br) throws IOException {
        var started = false;
        var finished = false;
        var header = "";

        // read line by line
        String line;
        while ((line = br.readLine()) != null) {
            if (line.startsWith("---") && !started) {
                started = true;
                continue;
            }

            if (line.startsWith("---")) {
                finished = true;
                break;
            }

            header += line + "\n";
        }

        if (finished) return header;
        return "";
    }

    public static String readContent(BufferedReader br) throws IOException {
        var content = "";

        // read line by line
        String line;
        while ((line = br.readLine()) != null) {
            if (content.isEmpty()) {
                content += line;
            } else {
                content += "\n" + line;
            }
        }

        return content;
    }

    public static void fetchFiles(File dir, Consumer<File> fileConsumer) {
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file1 : files) {
                    fetchFiles(file1, fileConsumer);
                }
            }
        } else if (
            dir.isFile() && dir.getName().toLowerCase().endsWith(".md")
        ) {
            fileConsumer.accept(dir);
        }
    }
	
}
