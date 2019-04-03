package org.redquark.autofileuploader.core.schedulers;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.scheduler.ScheduleOptions;
import org.apache.sling.commons.scheduler.Scheduler;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.redquark.autofileuploader.core.configs.AutoFileUploaderConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.dam.api.AssetManager;

/**
 * @author Anirudh Sharma
 * 
 *         This scheduler watches the specified directory and updates the assets
 */
@Component(service = Runnable.class, immediate = true)
@Designate(ocd = AutoFileUploaderConfiguration.class)
public class AutoFileUploader implements Runnable {

	// Logger
	private final Logger log = LoggerFactory.getLogger(this.getClass());

	// Instance of the OSGi configuration
	private AutoFileUploaderConfiguration autoFileUploaderConfiguration;

	// Defines the service API to get andcreate ResourceResolvers.
	@Reference
	private ResourceResolverFactory resourceResolverFactory;

	// A scheduler to schedule time/cron based jobs.A job is an object that is
	// executed/fired by the scheduler
	@Reference
	private Scheduler scheduler;

	// Scheduler Id to add/remove scheduler on modifications
	private String schedulerId;

	/**
	 * Initialize stuff
	 * 
	 * @param autoFileUploaderConfiguration
	 */
	@Activate
	protected void activate(AutoFileUploaderConfiguration autoFileUploaderConfiguration) {

		log.info("Intializing File Uploader service....");

		// Getting the instance of FileUploaderConfiguration
		this.autoFileUploaderConfiguration = autoFileUploaderConfiguration;

		// Creating the scheduler id - this will be needed for scheduling and
		// unscheduling whenever the configuration is modified
		schedulerId = UUID.randomUUID().toString();
	}

	/**
	 * This will be called when the configuration modifies
	 * 
	 * @param autoFileUploaderConfiguration
	 */
	@Modified
	protected void modified(AutoFileUploaderConfiguration autoFileUploaderConfiguration) {

		// Removing the scheduler
		removeScheduler();

		// Modify the schedulerId - Update
		schedulerId = UUID.randomUUID().toString();

		// Adding the scheduler again with updated id
		addScheduler();
	}

	/**
	 * This will be called when the configuration is removed
	 * 
	 * @param autoFileUploaderConfiguration
	 */
	@Deactivate
	protected void deactivate(AutoFileUploaderConfiguration autoFileUploaderConfiguration) {

		// Removing the scheduler
		removeScheduler();
	}

	/**
	 * Overridden method - which will perform the business logic
	 */
	@Override
	public void run() {

		log.info("Starting to watch for files...");

		// Poll the desired location for new files
		watchForFiles();
	}

	/**
	 * This method configures the scheduler and adds it.
	 */
	private void addScheduler() {

		try {

			// Provides options to create a scheduler
			ScheduleOptions scheduleOptions = scheduler.EXPR(autoFileUploaderConfiguration.cronExpression());

			// Setting name of the scheduler
			scheduleOptions.name(schedulerId);

			// Set concurrent run flag
			scheduleOptions.canRunConcurrently(false);

			// Schedule here
			scheduler.schedule(this, scheduleOptions);

		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	/**
	 * Method to remove scheduler
	 */
	private void removeScheduler() {

		log.info("Removing scheduler...");

		// Unscheduling
		scheduler.unschedule(schedulerId);
	}

	/**
	 * This method watches for files
	 */
	private final void watchForFiles() {

		// Representing input stream of bytes
		InputStream inputStream = null;

		try {

			// Getting the directory to watch out for
			String directory = autoFileUploaderConfiguration.watchedDirectoryPath();

			// Path
			Path path = Paths.get(directory);

			// A watch service that watches registered objects for changes and events. For
			// example a file manager may use a watch service to monitor a directory for
			// changes so that it can update its display of the list of files when files are
			// created or deleted.
			WatchService watchService = FileSystems.getDefault().newWatchService();

			// Registers the file located by this path with a watch service
			path.register(watchService, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);

			// This variable will store the name of the file to be uploaded in the DAM
			String fileName = null;

			log.info("Watch Service registered for directory: " + path.getFileName());

			// Getting the resource resolver
			ResourceResolver resourceResolver = getResourceResolver();

			while (true) {

				// A token representing the registration of a watchable object with a
				// WatchService. A watch key is created when a watchable object is registered
				// with a watch service.
				WatchKey watchKey;

				try {

					// Retrieves and removes next watch key, waiting if none are yet present
					watchKey = watchService.take();
				} catch (InterruptedException ie) {
					log.error(ie.getMessage(), ie);
					return;
				}

				log.info("Watcher service is running...");

				// An event or a repeated event for an object that is registered with a
				// WatchService.
				for (WatchEvent<?> event : watchKey.pollEvents()) {

					// Getting the kind of the event
					WatchEvent.Kind<?> kind = event.kind();

					// An event or a repeated event for an object that is registered with a
					// WatchService. An event is classified by its kind and has a count to indicate
					// the number of times that the event has been observed. This allows for
					// efficient representation of repeated events. The context method returns any
					// context associated with the event. In the case of a repeated event then the
					// context is the same for all events.
					@SuppressWarnings("unchecked")
					WatchEvent<Path> watchEvent = (WatchEvent<Path>) event;

					// Returns the context for the event. In the case of ENTRY_CREATE, ENTRY_DELETE,
					// and ENTRY_MODIFY events the context is a Path that is the relative path
					// between the directory registered with the watch service, and the entry that
					// is created, deleted, or modified.
					//
					// Here we will be watching the file that is modified/deleted/added
					Path filePath = watchEvent.context();

					if (kind == ENTRY_CREATE || kind == ENTRY_MODIFY || kind == ENTRY_DELETE) {

						// If new file is present
						fileName = filePath.toString();

						// Getting the File instance from the directory variable
						File folder = new File(directory);

						// Creating an array of all the files in the folder
						File[] files = folder.listFiles();

						// Iterating over the length of the array or check for each and every file that
						// is present in the watch dog folder
						for (int i = 0; i < files.length; i++) {

							// Creating a new instance of the file
							File file = files[i];

							// Creating the input steam object
							inputStream = new FileInputStream(file);

							// Save to AEM JCR
							saveToJCR(inputStream, fileName, resourceResolver);

						}
					}

					log.info("Watcher reset");

					// If this watch key has been cancelled or this watch key is already in the
					// ready
					// state then invoking this method has no effect. Otherwise if there are pending
					// events for the object then this watch key is immediately re-queued to the
					// watch service. If there are no pending events then the watch key is put into
					// the ready state and will remain in that state until an event is detected or
					// the watch key is cancelled.
					boolean valid = watchKey.reset();
					if (!valid) {
						break;
					}
				}
			}

		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			if (inputStream != null) {
				try {
					// Closing the stream
					inputStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * This will return the instance of resource resolver
	 * 
	 * @return {@link ResourceResolver}
	 */
	private final ResourceResolver getResourceResolver() {

		ResourceResolver resourceResolver = null;

		try {

			// Param map for the service user
			Map<String, Object> param = new HashMap<>();

			// Mapping service user with the sub-service
			param.put(ResourceResolverFactory.SUBSERVICE, "assetWrite");

			// Getting the resource resolver
			resourceResolver = resourceResolverFactory.getServiceResourceResolver(param);

			return resourceResolver;

		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return null;
		} finally {
			if (resourceResolver != null) {

				// Closing the resource resolver
				resourceResolver.close();
			}
		}
	}

	/**
	 * This method will save the asset into the JCR
	 * 
	 * @param is
	 * @param fileName
	 */
	private final void saveToJCR(InputStream is, String fileName, ResourceResolver resourceResolver) {

		try {

			// AssetManager will provide utility methods for assets
			AssetManager assetManager = resourceResolver.adaptTo(AssetManager.class);

			// Getting the fully qualified name of the asset to be saved
			String newFile = autoFileUploaderConfiguration.pathToSave() + "/" + fileName;

			// Creating an asset in the JCR
			assetManager.createAsset(newFile, is, "image/jpeg", true);

		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}
}
