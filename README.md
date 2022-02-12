# xtoys-color-detector

# Requirements: 

java 17

# Usage:

java XToysDetectColors.java webhookid color1 color2 ... color10

You can provide how many colors you want, as long as it more than one.

These will be sent to the webhook as keynames: color1, color2, color3, ...

Example:
  
java XToysDetectColors.java D2COE76wV65S #000000 #ff0000

You can google hex color picker if you aren't familiar with hex color codes.

If you want the color comparision to be more forgiving, than increase the MAX_DISTANCE variable in the top of the java file.
If you want the color comparision to be more strict, than decrease the MAX_DISTANCE variable in the top of the java file.
