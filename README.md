# Camera

Android Intent (with test driver) to replace the existing Android Intent in applications that need to take high resolution picture.  Setting the existing Camera intent to the highest resolution always crashes my apps on a Samsung Stratosphere II phone.  it has an 8 megapixel camera, and apparently does not have enough JVM heap space to pass the image back to the calling application as an Intent parameter.   

DONE:
* Enhance parts of the test driver to save/view old pictures

TODO:
* Make the UI nicer - use fragments to more effectively use the screen space when on a tablet.
