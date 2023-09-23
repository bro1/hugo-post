package com.bro1.hugopost;

import javafx.stage.FileChooser;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ResourceBundle;
import java.util.TimeZone;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.DragEvent;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.stage.Stage;

public class HugoPostHomePageController implements Initializable {


private static final String kInitialDir = "/home/bro1/projects/laisvamaniai/content/post";

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
	private void onCite(ActionEvent e) {
    	CiteController puc = new CiteController(this.text);    	
    	Utils.open("Cite.fxml", "Add Cite tag", puc, this.myStage);
    	
	}
	

	@FXML
    void onTitleChange(KeyEvent event) {

		// Only update the file name if this is a new file
		if (currentFile == null) {
			var t = title.getText();
			var d = new Date();
		
 	   		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
 	   		var ds = df.format(d);
 	    	   
 	   		SimpleDateFormat dfm = new SimpleDateFormat("yyyy/yyyy-MM");
 	   		var dsm = dfm.format(d); 	   
 	   		proposedFileName.setText(dsm + "/" + ds + " " + t + ".md"); 
		}
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
		FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("Markdown (*.md)", "*.md");
		fileChooser.getExtensionFilters().add(extFilter);
		fileChooser.setInitialDirectory(new File(kInitialDir));

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
    	
    	
	  try (BufferedReader in = new BufferedReader(new FileReader(currentFile))) {
		  
		  var header = readHeader(in);
		  var content = readContent(in);
		  System.out.println(header);
		  
		  Yaml y = new Yaml();
		  Object lll = y.load(header);
		  System.out.println(lll.getClass().getName());
		  
		  if (lll instanceof Map) {
			  	Map m = (Map)lll;
		        Object tags =  m.get("tags");
		        if (tags!= null)
		        	if (tags instanceof List<?>) {
		        		var tagsl = (List<String>)tags;
		        		for (var tag : tagsl) {		 
		        			//add(this.tags, tag);
		        			postTags.getItems().add(new Category(tag));
		        		}
		        		
		        		
		        	}
		        
		        Object cats =  m.get("categories");
		        if (cats!= null)
		        	if (cats instanceof List<?>) {
		        		var catsl = (List<String>)cats;
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
		        
		        
		        String title = (String)m.get("title");
		        this.title.setText(title);
		        
		        String desc = (String)m.get("description");
		        this.description.setText(desc);
		        
		        
		        Date postDate = (Date)m.get("date");
		        this.postDate = postDate;
		        System.out.println(postDate);
		  }
		  
		  
		  text.setText(content);
		  
		  
	  } catch (IOException e) {
		  System.out.println("Warning, cannot process file " + currentFile.getAbsolutePath());
	  }
	      	
    	
		
	}


	private String readContent(BufferedReader br) throws IOException {
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


	@FXML
    void onMenuSave(ActionEvent event) {

		if (currentFile == null) {
			
			currentFile = new File(kInitialDir + File.separator + proposedFileName.getText());
			

//			FileChooser fileChooser = new FileChooser();
//
//			// Set extension filter for text files
//			FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("Markdown (*.md)", "*.md");
//			fileChooser.getExtensionFilters().add(extFilter);
//			fileChooser.setInitialDirectory(new File(kInitialDir));
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
    	
    	   Map<String, Object> data = new HashMap<String, Object>();
    	   data.put("title", title);
    	   data.put("description", description);
    	   
    	   List<String> tags = postTags.getItems().stream()
    			   .map(Category::getName)
    			   .collect(Collectors.toList());    	   
    	   if (!tags.isEmpty()) {
    		   data.put("tags", tags.toArray(new String[] {}));
    	   }
    	   
    	   
    	   Category cat = category.getSelectionModel().getSelectedItem();
    	   if (cat != null) {
	    	   data.put("categories", new String[] {cat.name});	    	   
    	   }
    	   
    	   data.put("author", "Laisvamanis");
    	   
    	   var date  = postDate != null ? postDate : new Date();  
    	   
    	   // date: 2020-04-05T10:05:00+13:00
//    	   SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
//    	   var ds = df.format(date);
//    	   data.put("date", ds);
    	   
    	   data.put("date", date);
    	   
    	   
    	   
    	   DumperOptions options = new DumperOptions();
    	   options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
    	   options.setIndent(3);
    	   options.setIndicatorIndent(2);
    	   options.setTimeZone(TimeZone.getDefault());
    	   
    	   Yaml yaml = new Yaml(options);
    	    
    	   String yamloutput = yaml.dump(data);
//    	   System.out.println(yamloutput);
    	
    	   try (PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(currentFile)))) {
    		   
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
    void onSave(ActionEvent event) {

    	    	
    	
    }
    
    

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
		} else if (e.getSource() == description){
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
				if (tf!= null && dp != null) {
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
    
    
    void fetchFiles(File dir, Consumer<File> fileConsumer) {

        if (dir.isDirectory()) {
            for (File file1 : dir.listFiles()) {
                fetchFiles(file1, fileConsumer);
            }
        } else if (dir.isFile() && dir.getName().toLowerCase().endsWith(".md")){
            fileConsumer.accept(dir);
        }
    }    
	
  void readFiles() {
	  var f = new File(kInitialDir);
	  
	  fetchFiles(f, ff->proc(ff));
	  
	  
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

private LinkedHashMap<String, Integer> sort(HashMap<String, Integer> hashMap) {
	LinkedHashMap<String, Integer> sortedByValueMap = hashMap.entrySet().stream()
              .sorted(Comparator.comparing(Entry<String,Integer>::getValue).reversed().thenComparing(Entry::getKey))     
              .collect(LinkedHashMap::new,(map,entry) -> map.put(entry.getKey(),entry.getValue()),LinkedHashMap::putAll);
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
		  
		  var header = readHeader(in);
		  System.out.println(header);
		  
		  Yaml y = new Yaml();
		  Object lll = y.load(header);
		  System.out.println(lll.getClass().getName());
//		  System.out.println(lll.get);
		  
		  if (lll instanceof Map) {
			  	Map m = (Map)lll;
		        Object tags =  m.get("tags");
		        if (tags!= null)
		        	if (tags instanceof List<?>) {
		        		var tagsl = (List<String>)tags;
		        		for (var tag : tagsl) {
		        			System.out.println("TAG: " + tag);
		        			add(this.tags, tag);
		        		}
		        		
		        		
		        	}
		        
		        Object cats =  m.get("categories");
		        if (cats!= null)
		        	if (cats instanceof List<?>) {
		        		var catsl = (List<String>)cats;
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
		  System.out.println("Warning, cannot process file " + ff.getAbsolutePath());
	  }
	  
  }

    
private String readHeader(BufferedReader br) throws IOException {

	var started = false;
	var finished = false;
	var header = "";

    // read line by line
    String line;
    while ((line = br.readLine()) != null) {
        
    	if (line.startsWith("---") && !started) {
    		started=true;
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

@Override
  public void initialize(URL url, ResourceBundle rb) {
	  
	  myStage.setMinWidth(570);
	  myStage.setMinHeight(160);
	  readFiles();
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
  
}
