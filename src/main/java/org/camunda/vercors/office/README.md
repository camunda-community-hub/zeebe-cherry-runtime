# Office collection

Workers manipulate office document (Microsoft Office or Open office document).

# Generate office worker

The Worker uses an Office template and a map of variables to generate a document. The template document contains place
holder. Variables include the value for the placeholder For example, in the document, you can say

````
Hello @@NAME@@
````

In the variables, if you have NAME="Walter", then the document will be

````
Hello Walter
````

The placeholder mechanism used is Velocity (https://velocity.apache.org/engine/1.7/user-guide.html)
With this language, you can replace variables use a loop to build information from a list of data use conditional
placeholder The template is a Word document or an Open Office document. Use a Merge field in Word to add the Velocity
marker.