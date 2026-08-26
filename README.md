# hugo-post
Create Hugo blog posts (make sure that only the pre-existing tags and categoriers can be used)




I have successfully added the --tag flag to the application.

  Changes Summary:
   - Refactored File Operations: Moved readHeader, readContent, and fetchFiles methods to Utils.java to make them accessible globally.
   - Centralized Configuration: Moved the kInitialDir path to the Settings interface for consistent use across the application.
   - Added CLI Support: Updated HugoPostApp.java to detect the --tag flag. When present, the app scans all posts for the specified tag and concatenates their titles and content into a new Markdown file named after the tag (e.g., mytag.md).
   - Cleaned Up Controller: Updated HugoPostHomePageController.java to use the new centralized settings and utility methods, reducing code duplication.

  You can now use the flag like this:

   1 ./hugo-post.sh --tag <tagname>
  (Note: You may need to pass the argument through Maven if using the script directly, e.g., mvn javafx:run -Djavafx.args="--tag mytag")
