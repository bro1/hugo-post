package com.bro1.hugopost;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.List;
import java.util.Map;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.fxml.JavaFXBuilderFactory;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.yaml.snakeyaml.Yaml;

public class HugoPostApp extends Application {

	public static Stage myStage = null;

	public static void main(String[] args) {
		for (int i = 0; i < args.length; i++) {
			if ("--tag".equals(args[i]) && i + 1 < args.length) {
				String tag = args[i + 1];
				concatenatePostsByTag(tag);
				System.exit(0);
			}
		}
		Application.launch(HugoPostApp.class, args);
	}

	private static void concatenatePostsByTag(String tag) {
		File dir = new File(Settings.kInitialDir);
		File outputFile = new File(tag + ".md");

		try (PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(outputFile)))) {
			Utils.fetchFiles(dir, file -> {
				try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
					String header = Utils.readHeader(reader);
					if (header.isEmpty()) return;

					Yaml yaml = new Yaml();
					Map<String, Object> data = yaml.load(header);
					if (data == null) return;

					Object tagsObj = data.get("tags");
					if (tagsObj instanceof List) {
						List<String> tags = (List<String>) tagsObj;
						if (tags.contains(tag)) {
							String title = (String) data.get("title");
							String content = Utils.readContent(reader);

							writer.println("# " + title);
							writer.println();
							writer.println(content);
							writer.println();
							writer.println("---");
							writer.println();
						}
					}
				} catch (IOException e) {
					System.err.println("Error processing " + file.getAbsolutePath());
				}
			});
			System.out.println("Concatenated posts with tag '" + tag + "' into " + outputFile.getAbsolutePath());
		} catch (IOException e) {
			e.printStackTrace();
		}
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
