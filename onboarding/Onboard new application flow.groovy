/*
Format: Electric Flow DSL
File: Onboard new application flow.groovy
Description: Test setting properties from CLI and job step

Command-line run instructions
-----------------------------
ectool evalDsl --dslFile "Onboard new application flow.groovy"

*/
import groovy.json.JsonSlurper

def dslDir = "/vagrant/DSL-Samples/onboarding/"

project "Application Onboarding", {

	property "dslDir", value: dslDir
	property "stages", value: '[dev: "Development", qa: "Testing", st: "Staging", pr: "Release"]'
	
	procedure "Onboard new application flow",{
	
		// Parse in parameters from file
		def jsonSlurper = new JsonSlurper()
		def params = jsonSlurper.parseText(new File(dslDir + "parameters.json").text)
		def ec_parameterForm="<editor>\n"
		params.each { param, details ->
			def xmlType = "entry"
			formalParameter param, required: "1", type: xmlType // Default parameter type
			ec_parameterForm += "\t<formElement>\n"
			ec_parameterForm += "\t\t<property>$param</property>\n"
			details.each { k, v ->
				switch (k) {
					case "options":
						xmlType = "select"
						formalParameter param, type: xmlType
						ec_customEditorData.parameters.appTech.with {
							formType = "standard"
							options.with {
								type = "simpleList"
								list = v.join("|")
							}
						} // property sheet ec_customEditorData
						v.each { val ->
							ec_parameterForm += "\t\t\t<option>\n"
							ec_parameterForm += "\t\t\t\t<name>$val</name>\n"
							ec_parameterForm += "\t\t\t\t<value>$val</value>\n"
							ec_parameterForm += "\t\t\t</option>\n"
						}
						break
					case ["description","label","defaultValue"]:
						formalParameter param, (k): v
						if (k=="description") k="documentation"
						if (k=="defaultValue") k="value"
						ec_parameterForm += "\t\t<$k>$v</$k>\n"
						break
				}
			}
			ec_parameterForm += "\t\t<type>$xmlType</type>\n"		
			ec_parameterForm += "\t</formElement>\n"
		}​
		ec_parameterForm += "</editor>\n"
		property "ec_parameterForm", value: ec_parameterForm
		// End of parse in parameters
		
		step "Generate Procedures",
			description: "Create Build, Snapshot, Unit Test, System Test procedures",
			command: new File(dslDir + "genProcedures.groovy").text,
			shell: "ectool evalDsl --dslFile {0}"
		step "Generate Environment and Application Models",
			command: new File(dslDir + "genApp.groovy").text,
			shell: "ectool evalDsl --dslFile {0}"
		step "Generate Pipeline",
			command: new File(dslDir + "genPipe.groovy").text,
			shell: "ectool evalDsl --dslFile {0}"
		step "Generate CI Configuration",
			command: new File(dslDir + "genCi.groovy").text,
			shell: "ectool evalDsl --dslFile {0}"
		
	} // Procedure "Onboard new application flow"
}