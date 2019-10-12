
LIBDIR=..\lib\jasypt-1.9.3\lib\jasypt-1.9.3.jar

PwdAwMan.class: PwdAwMan.java
	javac -cp $(LIBDIR) PwdAwMan.java 

