package com.bro1.hugopost;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;


public class CiteController extends AbstractStandardController {
	

	public CiteController(TextArea textArea) {
		super();
		this.textArea = textArea;
	}

	protected TextArea textArea;

	@Override
	public void initialize(URL arg0, ResourceBundle arg1) {

	}
	
	
    @FXML
    private Button btnCancel;
    
    @FXML
    private Button btnSave;


    @FXML
    private TextArea description;


    @FXML
    private TextField url;
    
    @FXML
    private TextField text;
    

    @FXML
	void onSave(ActionEvent event) {
		
    	var textstring = text.getText();
    	var urlstring = url.getText();
   		
    	if (!textstring.isBlank() && !urlstring.isBlank()) {
    		var start = textArea.getSelection().getStart();
    		textArea.insertText(start, "> -- <cite>[" + textstring + "](" + urlstring +")</cite>\n" );
    	}
    		
    	getStage().close();
	}

    @FXML
	void onCancel(ActionEvent event) {
		getStage().close();
	}
    
}
