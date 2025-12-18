package com.WebSu.ig;

public enum TerminalOutputType {

        TYPE_COMMAND("Output TYPE_COMMAND", "Save TYPE_COMMAND"), 
        TYPE_START("Output TYPE_START", "Save TYPE_START"),   
        TYPE_EXIT("Output TYPE_EXIT", "Save TYPE_EXIT"),     
        TYPE_STDOUT("Output TYPE_STDOUT", "Save TYPE_STDOUT"), 
        TYPE_STDERR("Output TYPE_STDERR", "Save TYPE_STDERR"), 
        TYPE_THROW("Output TYPE_THROW", "Save TYPE_THROW"),
        TYPE_SPACE("Output TYPE_SPACE", "Save TYPE_SPACE"), 
        TYPE_STDIN("Output TYPE_STDIN", "Save TYPE_STDIN");  

        private final String descriptionOutput;
        private final String descriptionSave;

        TerminalOutputType(String descriptionOutput, String descriptionSave) {
                this.descriptionOutput = descriptionOutput;
                this.descriptionSave = descriptionSave;
            }

        public String getDescriptionOutput() {
                return descriptionOutput;
            }

        public String getDescriptionSave() {
                return descriptionSave;
            }

        public String getDescription() {
                return descriptionOutput;
            }
    }

