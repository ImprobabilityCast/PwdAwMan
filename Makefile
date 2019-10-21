
# make (or more likely, my command processor) likes semicolons to mean other things
LIBS=../lib/jasypt-1.9.3/lib/jasypt-1.9.3.jar\;.
DIR=improbabilitycast/pwdawman/

all: $(DIR)PwdAwMan.class $(DIR)DisplayUtil.class $(DIR)ParseUtil.class

$(DIR)PwdAwMan.class: $(DIR)DisplayUtil.class $(DIR)ParseUtil.class $(DIR)PwdAwMan.java
	javac -cp $(LIBS) $(DIR)PwdAwMan.java

$(DIR)DisplayUtil.class: $(DIR)DisplayUtil.java
	javac -cp $(LIBS) $(DIR)DisplayUtil.java

$(DIR)ParseUtil.class: $(DIR)ParseUtil.java
	javac -cp $(LIBS) $(DIR)ParseUtil.java

clean:
	rm $(DIR)PwdAwMan.class $(DIR)DisplayUtil.class $(DIR)ParseUtil.class
