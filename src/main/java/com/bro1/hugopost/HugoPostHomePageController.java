package com.bro1.hugopost;

import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ResourceBundle;
import java.util.TimeZone;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.GridPane;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.DragEvent;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

public class HugoPostHomePageController implements Initializable {

    public Stage myStage;

    private HashMap<String, Integer> tags = new HashMap<>();
    private HashMap<String, Integer> categories = new HashMap<>();

    private File currentFile = null;
    private Date postDate = null;

    @FXML
    private TextArea text;

    @FXML
    private ListView<Category> postTags;

    @FXML
    private ListView<Category> allTags;

    @FXML
    private TextField newTag;

    @FXML
    private TextField title;

    @FXML
    private TextField description;

    @FXML
    private ChoiceBox<Category> category;

    @FXML
    private TextField proposedFileName;

    @FXML
    private void onMenuExit(ActionEvent e) {
        Platform.exit();
    }

    @FXML
    private void onMenuPostToMastodon(ActionEvent e) {
        String t = urlenc(title.getText());
        String a = urlenc(text.getText());
        String enc = "";

        if (t.isBlank()) {
            enc = a;
        } else {
            enc = t + "%0A%0A" + a;
        }

        String url = "https://mas.to/share/?text=" + enc;
        launchBrowser(url);
    }

    private String urlenc(String a) {
        String enc = "";
        try {
            enc = URLEncoder.encode(a, StandardCharsets.UTF_8.toString());
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return enc;
    }

    @FXML
    private void onMenuPostToWordPress(ActionEvent e) {
        String t = urlenc(title.getText());
        String enc = urlenc(text.getText());

        String url =
            "https://laisvamaniai.wordpress.com/wp-admin/post-new.php?post_title=" +
            t +
            "&content=" +
            enc;
        launchBrowser(url);
    }



    @FXML
    private void onMenuFacebookOnboarding(ActionEvent e) {
        showFacebookOnboardingDialog();
    }

    private void showFacebookOnboardingDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Facebook OAuth Onboarding");
        dialog.setHeaderText("Automate Facebook Page Onboarding\n"
                + "Follow instructions in specs/f-02 facebook oauth onboarding.md");

        ButtonType onboardButtonType = new ButtonType("Automate Onboarding", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(onboardButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        FacebookOnboarder.SpecDefaults defaults = FacebookOnboarder.loadSpecDefaults();

        TextField appIdField = new TextField(defaults.appId);
        appIdField.setPromptText("Meta App ID");

        PasswordField appSecretField = new PasswordField();
        appSecretField.setText(defaults.appSecret);
        appSecretField.setPromptText("Meta App Secret");

        TextField tokenField = new TextField(defaults.clientToken);
        tokenField.setPromptText("User Access Token / Client Token");

        String initialPageId = "";
        try {
            FacebookConfig existingConfig = FacebookConfig.loadFromHomeDir();
            if (existingConfig.getPageId() != null) {
                initialPageId = existingConfig.getPageId();
            }
        } catch (Exception ignored) {}

        TextField pageIdField = new TextField(initialPageId);
        pageIdField.setPromptText("Facebook Page ID (Optional)");

        Button openExplorerBtn = new Button("Open Meta Graph API Explorer");
        openExplorerBtn.setOnAction(evt -> launchBrowser("https://developers.facebook.com/tools/explorer/"));

        grid.add(new Label("Meta App ID:"), 0, 0);
        grid.add(appIdField, 1, 0);
        grid.add(new Label("Meta App Secret:"), 0, 1);
        grid.add(appSecretField, 1, 1);
        grid.add(new Label("User Access Token:"), 0, 2);
        grid.add(tokenField, 1, 2);
        grid.add(new Label("Page ID (Optional):"), 0, 3);
        grid.add(pageIdField, 1, 3);
        grid.add(openExplorerBtn, 1, 4);

        dialog.getDialogPane().setContent(grid);

        dialog.showAndWait().ifPresent(response -> {
            if (response.getButtonData() == ButtonBar.ButtonData.OK_DONE) {
                String appId = appIdField.getText() != null ? appIdField.getText().trim() : "";
                String appSecret = appSecretField.getText() != null ? appSecretField.getText().trim() : "";
                String userToken = tokenField.getText() != null ? tokenField.getText().trim() : "";
                String pageId = pageIdField.getText() != null ? pageIdField.getText().trim() : "";

                if (appId.isEmpty() || appSecret.isEmpty() || userToken.isEmpty()) {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Onboarding Error");
                    alert.setHeaderText("Missing Information");
                    alert.setContentText("App ID, App Secret, and User Access Token are all required.");
                    alert.showAndWait();
                    return;
                }

                FacebookOnboarder onboarder = new FacebookOnboarder();
                CompletableFuture.supplyAsync(() -> {
                    try {
                        String longLivedToken = onboarder.exchangeForLongLivedToken(appId, appSecret, userToken);
                        List<FacebookOnboarder.PageInfo> pages = onboarder.fetchManagedPages(longLivedToken, pageId);
                        return pages;
                    } catch (Exception ex) {
                        throw new RuntimeException(ex);
                    }
                }).thenAccept(pages -> Platform.runLater(() -> {
                    FacebookOnboarder.PageInfo selectedPage = null;
                    if (pages.size() == 1) {
                        selectedPage = pages.get(0);
                    } else if (pages.size() > 1) {
                        ChoiceDialog<FacebookOnboarder.PageInfo> choiceDialog = new ChoiceDialog<>(pages.get(0), pages);
                        choiceDialog.setTitle("Select Facebook Page");
                        choiceDialog.setHeaderText("Multiple Facebook Pages Found");
                        choiceDialog.setContentText("Choose the page you want to post to:");
                        Optional<FacebookOnboarder.PageInfo> result = choiceDialog.showAndWait();
                        if (result.isPresent()) {
                            selectedPage = result.get();
                        }
                    }

                    if (selectedPage != null) {
                        try {
                            FacebookOnboarder.saveConfig(appId, appSecret, userToken, selectedPage.getId(), selectedPage.getAccessToken());
                            Alert alert = new Alert(Alert.AlertType.INFORMATION);
                            alert.setTitle("Onboarding Successful");
                            alert.setHeaderText("Facebook Page Onboarding Complete!");
                            alert.setContentText("Page Name: " + selectedPage.getName() + "\nPage ID: " + selectedPage.getId() + "\n\nSaved credentials to ~/.hugopost");
                            alert.showAndWait();
                        } catch (IOException ioEx) {
                            Alert alert = new Alert(Alert.AlertType.ERROR);
                            alert.setTitle("Onboarding Error");
                            alert.setHeaderText("Failed to save configuration");
                            alert.setContentText(ioEx.getMessage());
                            alert.showAndWait();
                        }
                    }
                })).exceptionally(ex -> {
                    Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("Onboarding Failed");
                        alert.setHeaderText("Failed to complete Facebook OAuth onboarding");
                        alert.setContentText(ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage());
                        alert.showAndWait();
                    });
                    return null;
                });
            }
        });
    }

    @FXML
    private void onMenuPostToFacebook(ActionEvent e) {
        FacebookConfig config;
        try {
            config = FacebookConfig.loadFromHomeDir();
        } catch (Exception ex) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Facebook Configuration Missing");
            alert.setHeaderText("Facebook configuration is not set up.");
            alert.setContentText(ex.getMessage() + "\n\nWould you like to start the automated onboarding wizard now?");
            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                showFacebookOnboardingDialog();
            }
            return;
        }

        String postTitle = title != null && title.getText() != null ? title.getText().trim() : "";
        String postText = text != null && text.getText() != null ? text.getText().trim() : "";

        if (postTitle.isEmpty() && postText.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Facebook Post");
            alert.setHeaderText("Empty Post");
            alert.setContentText("Please provide a title or content before posting to Facebook.");
            alert.showAndWait();
            return;
        }

        String message = postTitle.isEmpty() ? postText : (postText.isEmpty() ? postTitle : postTitle + "\n\n" + postText);

        String firstUrl = extractFirstUrl(postText);
        if (firstUrl == null) {
            firstUrl = extractFirstUrl(postTitle);
        }

        StringBuilder requestBodyBuilder = new StringBuilder();
        requestBodyBuilder.append("message=").append(URLEncoder.encode(message, StandardCharsets.UTF_8));
        requestBodyBuilder.append("&access_token=").append(URLEncoder.encode(config.getAccessToken(), StandardCharsets.UTF_8));

        if (firstUrl != null && !firstUrl.isBlank()) {
            requestBodyBuilder.append("&link=").append(URLEncoder.encode(firstUrl.trim(), StandardCharsets.UTF_8));
        }

        HttpClient client = HttpClient.newHttpClient();
        String requestBody = requestBodyBuilder.toString();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://graph.facebook.com/v19.0/" + config.getPageId() + "/feed"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        CompletableFuture.runAsync(() -> {
            try {
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                Platform.runLater(() -> {
                    if (response.statusCode() == 200 || response.statusCode() == 201) {
                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setTitle("Facebook Post Success");
                        alert.setHeaderText("Post published to Facebook successfully!");
                        alert.setContentText("Response:\n" + response.body());
                        alert.showAndWait();
                    } else {
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("Facebook Post Failed");
                        alert.setHeaderText("Facebook Graph API returned HTTP " + response.statusCode());
                        alert.setContentText(response.body());
                        alert.showAndWait();
                    }
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Facebook Post Error");
                    alert.setHeaderText("Failed to send HTTP request to Facebook API");
                    alert.setContentText(ex.getMessage());
                    alert.showAndWait();
                });
            }
        });
    }

    @FXML
    private void onMenuPostToBluesky(ActionEvent e) {


        String t = urlenc(title.getText());
        String a = urlenc(text.getText());
        String enc = "";

        if (t.isBlank()) {
            enc = a;
        } else {
            enc = t + "%0A%0A" + a;
        }

        String url = "https://bsky.app/intent/compose?text=" + enc;
        launchBrowser(url);

    }

    @FXML
    private void onCite(ActionEvent e) {
        CiteController puc = new CiteController(this.text);
        Utils.open("Cite.fxml", "Add Cite tag", puc, this.myStage);
    }

    @FXML
    void onTitleChange(KeyEvent event) {
        // Only update the file name if this is a new file
        if (currentFile == null) {
            var t = title.getText();

            t = slug(t);

            var d = new Date();

            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
            var ds = df.format(d);

            SimpleDateFormat dfm = new SimpleDateFormat("yyyy/yyyy-MM");
            var dsm = dfm.format(d);
            proposedFileName.setText(dsm + "/" + ds + " " + t + ".md");
        }
    }

    private String slug(String t) {
        String from = "ąčęėįšųūžĄČĘĖĮŠŲŪŽ";
        String to = "aceeisuuzACEEISUUZ";

        String n = t;

        for (int i = 0; i < from.length(); i++) {
            n = n.replaceAll(
                new String(from.substring(i, i + 1)),
                to.substring(i, i + 1)
            );
        }

        return n;
    }

    @FXML
    void onTagsAction(ActionEvent event) {
        // add if this tag does not exist yet

        var newTagText = newTag.getText();
        if (newTagText.isBlank()) return;

        for (var v : postTags.getItems()) {
            if (v.name.equals(newTagText)) return;
        }

        postTags.getItems().add(new Category(newTagText));
    }

    @FXML
    void onMenuOpen(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();

        // Set extension filter for text files
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter(
            "Markdown (*.md)",
            "*.md"
        );
        fileChooser.getExtensionFilters().add(extFilter);

        var d = new Date();
        SimpleDateFormat dfm = new SimpleDateFormat("yyyy/yyyy-MM");
        var dsm = dfm.format(d);

        String initialDirCurrentMonthStr = Settings.kInitialDir + "/" + dsm;

        File initialDirCurrentMonthFile = new File(initialDirCurrentMonthStr);

        if (initialDirCurrentMonthFile.exists()) {
            fileChooser.setInitialDirectory(initialDirCurrentMonthFile);
        } else {
            fileChooser.setInitialDirectory(new File(Settings.kInitialDir));
        }

        // Show save file dialog
        File file = fileChooser.showOpenDialog(myStage);

        if (file != null) {
            currentFile = file;
            // Display this in the dialog
            proposedFileName.setText(currentFile.getAbsolutePath());
        }

        doLoad();
    }

    private void doLoad() {
        postTags.getItems().clear();
        category.getSelectionModel().clearSelection();
        text.clear();

        try (
            BufferedReader in = new BufferedReader(new FileReader(currentFile))
        ) {
            var header = Utils.readHeader(in);
            var content = Utils.readContent(in);
            System.out.println(header);

            Yaml y = new Yaml();
            Object lll = y.load(header);
            System.out.println(lll.getClass().getName());

            if (lll instanceof Map) {
                Map m = (Map) lll;
                Object tags = m.get("tags");
                if (tags != null) if (tags instanceof List<?>) {
                    var tagsl = (List<String>) tags;
                    for (var tag : tagsl) {
                        //add(this.tags, tag);
                        postTags.getItems().add(new Category(tag));
                    }
                }

                Object cats = m.get("categories");
                if (cats != null) if (cats instanceof List<?>) {
                    var catsl = (List<String>) cats;
                    for (var cat : catsl) {
                        System.out.println("Category: " + cat);
                        var list = category.getItems();
                        for (var c : list) {
                            if (c.name.equals(cat)) {
                                category.getSelectionModel().select(c);
                            }
                        }
                    }
                }

                String title = (String) m.get("title");
                this.title.setText(title);

                String desc = (String) m.get("description");
                this.description.setText(desc);

                Date postDate = (Date) m.get("date");
                this.postDate = postDate;
                System.out.println(postDate);
            }

            text.setText(content);
        } catch (IOException e) {
            System.out.println(
                "Warning, cannot process file " + currentFile.getAbsolutePath()
            );
        }
    }

    @FXML
    void onMenuSave(ActionEvent event) {
        if (currentFile == null) {
            currentFile = new File(
                Settings.kInitialDir + File.separator + proposedFileName.getText()
            );

            //			FileChooser fileChooser = new FileChooser();
            //
            //			// Set extension filter for text files
            //			FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("Markdown (*.md)", "*.md");
            //			fileChooser.getExtensionFilters().add(extFilter);
            //			fileChooser.setInitialDirectory(new File(Settings.kInitialDir));
            //
            //			// Show save file dialog
            //			File file = fileChooser.showSaveDialog(myStage);
            //
            //			if (file != null) {
            //
            //				currentFile = file;
            //
            //			}
        }

        doSave();
    }

    private void doSave() {
        String title = this.title.getText();
        String description = this.description.getText();

        Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("title", title);
        data.put("description", description);

        var date = postDate != null ? postDate : new Date();
        data.put("date", date);

        data.put("author", "Laisvamanis");

        Category cat = category.getSelectionModel().getSelectedItem();
        if (cat != null) {
            data.put("categories", new String[] { cat.name });
        }

        List<String> tags = postTags
            .getItems()
            .stream()
            .map(Category::getName)
            .collect(Collectors.toList());
        if (!tags.isEmpty()) {
            data.put("tags", tags.toArray(new String[] {}));
        }

        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setIndent(3);
        options.setIndicatorIndent(2);
        options.setTimeZone(TimeZone.getDefault());

        Yaml yaml = new Yaml(options);

        String yamloutput = yaml.dump(data);

        try (
            PrintWriter out = new PrintWriter(
                new BufferedWriter(new FileWriter(currentFile))
            )
        ) {
            out.println("---");
            out.println(yamloutput);
            out.println("---");
            out.println(text.getText());
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @FXML
    void onSave(ActionEvent event) {}

    @FXML
    void tagsKeyTyped(KeyEvent event) {
        // filter out the all tags
        var filterText = newTag.getText();
        loadFilteredTagsList(filterText);
    }

    @FXML
    private void onDragDropped(DragEvent e) {
        TextField tf = null;

        if (e.getSource() == title) {
            tf = title;
        } else if (e.getSource() == description) {
            tf = description;
        }

        String s = e.getDragboard().getString();

        URL u = null;
        try {
            u = new URL(s);
        } catch (MalformedURLException e1) {
            e1.printStackTrace();
        }

        if (u != null) {
            String protocol = u.getProtocol();

            if ("file".equals(protocol)) {
                String p = u.getPath();
                String dp = null;
                try {
                    dp = URLDecoder.decode(p, "utf-8");
                } catch (UnsupportedEncodingException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }
                System.out.println(dp);
                if (tf != null && dp != null) {
                    tf.setText(dp);
                }
            }
        }

        e.consume();
    }

    @FXML
    private void onDragOver(DragEvent e) {
        e.acceptTransferModes(TransferMode.COPY_OR_MOVE);
    }

    String getFileName(String fullPathToFile) {
        File f = new File(fullPathToFile);
        return f.getName();
    }

    void readFiles() {
        var f = new File(Settings.kInitialDir);

        Utils.fetchFiles(f, ff -> proc(ff));

        this.category.getItems().clear();

        LinkedHashMap<String, Integer> sortedCategories = sort(categories);

        for (var e : sortedCategories.entrySet()) {
            Category c = new Category();
            c.name = e.getKey();
            c.count = e.getValue();
            this.category.getItems().add(c);
        }

        loadFilteredTagsList("");
    }

    private void loadFilteredTagsList(String filter) {
        allTags.getItems().clear();

        LinkedHashMap<String, Integer> sortedTags = sort(tags);

        for (var e : sortedTags.entrySet()) {
            if (filter.isBlank() || e.getKey().contains(filter)) {
                Category c = new Category();
                c.name = e.getKey();
                c.count = e.getValue();
                this.allTags.getItems().add(c);
            }
        }
    }

    private LinkedHashMap<String, Integer> sort(
        HashMap<String, Integer> hashMap
    ) {
        LinkedHashMap<String, Integer> sortedByValueMap = hashMap
            .entrySet()
            .stream()
            .sorted(
                Comparator.comparing(Entry<String, Integer>::getValue)
                    .reversed()
                    .thenComparing(Entry::getKey)
            )
            .collect(
                LinkedHashMap::new,
                (map, entry) -> map.put(entry.getKey(), entry.getValue()),
                LinkedHashMap::putAll
            );
        return sortedByValueMap;
    }

    private void add(Map<String, Integer> map, String value) {
        if (map.containsKey(value)) {
            Integer i = map.get(value);
            //map.remove(value);
            map.put(value, ++i);
        } else {
            map.put(value, 1);
        }
    }

    private void proc(File ff) {
        System.out.println(ff.getAbsolutePath());

        try (BufferedReader in = new BufferedReader(new FileReader(ff))) {
            var header = Utils.readHeader(in);
            System.out.println(header);

            Yaml y = new Yaml();
            Object lll = y.load(header);
            System.out.println(lll.getClass().getName());
            //		  System.out.println(lll.get);

            if (lll instanceof Map) {
                Map m = (Map) lll;
                Object tags = m.get("tags");
                if (tags != null) if (tags instanceof List<?>) {
                    var tagsl = (List<String>) tags;
                    for (var tag : tagsl) {
                        System.out.println("TAG: " + tag);
                        add(this.tags, tag);
                    }
                }

                Object cats = m.get("categories");
                if (cats != null) if (cats instanceof List<?>) {
                    var catsl = (List<String>) cats;
                    for (var cat : catsl) {
                        System.out.println("Category: " + cat);
                        add(this.categories, cat);
                    }
                }
            }

            //		  TomlParseResult p = Toml.parse(header);
            //		  TomlArray ss = p.get("tags");
            //		  if (ss!= null && ss.containsStrings()) {
            //			  for (int i = 0; i <= ss.size(); i++) {
            //				  System.out.println("TAG " + ss.getString(i));
            //
            //			  }
            //		  }
        } catch (IOException e) {
            System.out.println(
                "Warning, cannot process file " + ff.getAbsolutePath()
            );
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        myStage.setMinWidth(570);
        myStage.setMinHeight(160);
        readFiles();
    }

    @FXML
    void onPostTags(MouseEvent event) {
        // when double clicked - remove selected item from the post tags

        if (event.getClickCount() >= 2) {
            var v = postTags.getSelectionModel().getSelectedItem();
            postTags.getItems().remove(v);
        }
    }

    @FXML
    void onAllTags(MouseEvent event) {
        var v = allTags.getSelectionModel().getSelectedItem();

        // check if this is already in the postTags
        // return if it is
        for (var t : postTags.getItems()) {
            if (v.name.equals(t.name)) {
                return;
            }
        }

        postTags.getItems().add(v.clone());
    }

    public void launchBrowser(String targeturl) {
        List<String> command = new LinkedList<String>();

        String os = System.getProperty("os.name").toLowerCase();
        var isWindows = os.indexOf("win") >= 0;
        var isMac = os.indexOf("mac") >= 0;
        var isLinuxOrUnix = os.indexOf("nix") >= 0 || os.indexOf("nux") >= 0;

        if (currentBrowser.equals("default")) {
            if (isLinuxOrUnix) {
                command.add("xdg-open");
                command.add(targeturl);

                try {
                    new ProcessBuilder(command).start();
                } catch (Throwable t) {
                    t.printStackTrace(System.err);
                }
            } else if (isWindows || isMac) {
                try {
                    Desktop.getDesktop().browse(new URI(targeturl));
                } catch (Throwable t) {
                    t.printStackTrace(System.err);
                }
            }
        } else {
            List<String> l = null;
            if (isLinuxOrUnix) {
                l = browsers.get(currentBrowser);
            } else if (isWindows) {
                l = winBrowsers.get(currentBrowser);
            }

            command.addAll(l);
            command.add(targeturl);

            try {
                System.out.println(targeturl);
                new ProcessBuilder(command).start();
            } catch (Throwable th) {
                th.printStackTrace(System.err);
            }
        }
    }

    public String currentBrowser = "default";

    public Map<String, List<String>> browsers = new HashMap<>();

    {
        browsers.put(
            "chrome anonymous",
            List.of("google-chrome", "--incognito")
        );
        browsers.put("chrome", List.of("google-chrome"));
        browsers.put("firefox", List.of("firefox"));
        browsers.put("firefox private", List.of("firefox", "-private-window"));
    }

    public Map<String, List<String>> winBrowsers = new HashMap<>();

    {
        winBrowsers.put(
            "chrome anonymous",
            List.of(
                "C:/Program Files (x86)/Google/Chrome/Application/chrome.exe",
                "--incognito"
            )
        );
        winBrowsers.put(
            "chrome",
            List.of(
                "C:/Program Files (x86)/Google/Chrome/Application/chrome.exe"
            )
        );
        winBrowsers.put(
            "firefox",
            List.of("C:/Program Files/Mozilla Firefox/firefox.exe")
        );
        winBrowsers.put(
            "firefox private",
            List.of(
                "c:/Program Files/Mozilla Firefox/firefox.exe",
                "-private-window"
            )
        );
    }

    public static String extractFirstUrl(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        // 1. Check for Markdown link [label](http://...)
        Pattern mdPattern = Pattern.compile("\\[.*?\\]\\((https?://[^\\s\\)]+)\\)", Pattern.CASE_INSENSITIVE);
        Matcher mdMatcher = mdPattern.matcher(text);
        if (mdMatcher.find()) {
            return mdMatcher.group(1);
        }

        // 2. Check for plain HTTP/HTTPS URL
        Pattern urlPattern = Pattern.compile("https?://[^\\s>\\]\\)]+", Pattern.CASE_INSENSITIVE);
        Matcher urlMatcher = urlPattern.matcher(text);
        if (urlMatcher.find()) {
            return urlMatcher.group();
        }

        return null;
    }
}
