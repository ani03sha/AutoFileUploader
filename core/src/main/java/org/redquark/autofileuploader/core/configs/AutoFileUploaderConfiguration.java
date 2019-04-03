package org.redquark.autofileuploader.core.configs;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 * @author Anirudh Sharma
 * 
 * This interface defines the configuration for the watched folder scheduler
 */
@ObjectClassDefinition(name = "Red Quark Auto File Uploader Configuration", description = "Configure for file upload options")
public @interface AutoFileUploaderConfiguration {

	@AttributeDefinition(name = "Watcher Directory Path", description = "Enter the path of the directory to watched for", type = AttributeType.STRING)
	public String watchedDirectoryPath();

	@AttributeDefinition(name = "JCR Path", description = "Path to save binary in the AEM JCR", type = AttributeType.STRING)
	public String pathToSave() default "/content/dam";

	@AttributeDefinition(name = "Cron Expression", description = "Enter the cron expression to run the scheduler", type = AttributeType.STRING)
	public String cronExpression() default "0 * * * * ?";
}
