package com.github.czyzby.autumn.mvc.component.ui;

import java.util.Locale;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.loaders.BitmapFontLoader.BitmapFontParameter;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.scenes.scene2d.Action;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.I18NBundle;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.ObjectMap.Entry;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.github.czyzby.autumn.annotation.field.Inject;
import com.github.czyzby.autumn.annotation.method.Destroy;
import com.github.czyzby.autumn.annotation.method.Initiate;
import com.github.czyzby.autumn.annotation.stereotype.Component;
import com.github.czyzby.autumn.error.AutumnRuntimeException;
import com.github.czyzby.autumn.mvc.component.asset.AssetService;
import com.github.czyzby.autumn.mvc.component.i18n.LocaleService;
import com.github.czyzby.autumn.mvc.component.sfx.MusicService;
import com.github.czyzby.autumn.mvc.component.ui.action.ActionProvider;
import com.github.czyzby.autumn.mvc.component.ui.action.ApplicationExitAction;
import com.github.czyzby.autumn.mvc.component.ui.action.ApplicationPauseAction;
import com.github.czyzby.autumn.mvc.component.ui.action.ApplicationResumeAction;
import com.github.czyzby.autumn.mvc.component.ui.action.CommonActionRunnables;
import com.github.czyzby.autumn.mvc.component.ui.action.DialogShowingAction;
import com.github.czyzby.autumn.mvc.component.ui.action.MusicFadingAction;
import com.github.czyzby.autumn.mvc.component.ui.action.ScreenTransitionAction;
import com.github.czyzby.autumn.mvc.component.ui.controller.ViewController;
import com.github.czyzby.autumn.mvc.component.ui.controller.ViewDialogController;
import com.github.czyzby.autumn.mvc.component.ui.controller.ViewInitializer;
import com.github.czyzby.autumn.mvc.component.ui.controller.ViewPauser;
import com.github.czyzby.autumn.mvc.component.ui.controller.ViewRenderer;
import com.github.czyzby.autumn.mvc.component.ui.controller.ViewResizer;
import com.github.czyzby.autumn.mvc.component.ui.controller.ViewShower;
import com.github.czyzby.autumn.mvc.component.ui.controller.impl.StandardCameraCenteringViewResizer;
import com.github.czyzby.autumn.mvc.component.ui.controller.impl.StandardViewRenderer;
import com.github.czyzby.autumn.mvc.component.ui.controller.impl.StandardViewShower;
import com.github.czyzby.autumn.mvc.component.ui.dto.SkinData;
import com.github.czyzby.autumn.mvc.component.ui.dto.ViewActionProvider;
import com.github.czyzby.autumn.mvc.component.ui.processor.I18nBundleAnnotationProcessor;
import com.github.czyzby.autumn.mvc.component.ui.processor.SkinAnnotationProcessor;
import com.github.czyzby.autumn.mvc.component.ui.processor.ViewActionContainerAnnotationProcessor;
import com.github.czyzby.autumn.mvc.config.AutumnActionPriority;
import com.github.czyzby.kiwi.util.common.Strings;
import com.github.czyzby.kiwi.util.gdx.asset.lazy.provider.ObjectProvider;
import com.github.czyzby.kiwi.util.gdx.collection.GdxArrays;
import com.github.czyzby.kiwi.util.gdx.collection.GdxMaps;
import com.github.czyzby.kiwi.util.gdx.file.CommonFileExtension;
import com.github.czyzby.kiwi.util.gdx.preference.ApplicationPreferences;
import com.github.czyzby.lml.parser.LmlParser;
import com.github.czyzby.lml.parser.impl.dto.ActionContainer;
import com.github.czyzby.lml.parser.impl.dto.ActorConsumer;
import com.github.czyzby.lml.util.Lml;

/** Manages view controllers and a LML parser.
 *
 * @author MJ */
@Component
public class InterfaceService {
	/** Defines default resizing behavior. Can be modified statically before context initiation to set default
	 * behavior for controllers that do not implement this interface. */
	public static ViewResizer DEFAULT_VIEW_RESIZER = new StandardCameraCenteringViewResizer();
	/** Defines default showing and hiding behavior. Can be modified statically before context initiation to
	 * set default behavior for controllers that do not implement this interface. */
	public static ViewShower DEFAULT_VIEW_SHOWER = new StandardViewShower();
	/** Defines default rendering behavior. Can be modified statically before context initiation to set default
	 * behavior for controllers that do not implement this interface. */
	public static ViewRenderer DEFAULT_VIEW_RENDERER = new StandardViewRenderer();
	/** Defines default pausing and resuming behavior. Can be modified statically before context initiation to
	 * set default behavior for controllers that do not implement this interface. Defaults to null. */
	public static ViewPauser DEFAULT_VIEW_PAUSER = null;
	/** Defines default initializing and destroying behavior. Can be modified statically before context
	 * initiation to set default behavior for controllers that do not implement this interface. Defaults to
	 * null. */
	public static ViewInitializer DEFAULT_VIEW_INITIALIZER = null;

	/** Defaults prefix used to add dialog transition actions to LML parser upon dialog controllers'
	 * registrations. Can be modified statically before context initiation to use another prefix. */
	public static String DIALOG_SHOWING_ACTION_PREFIX = "show:";
	/** Default prefix used to add screen transition actions to the LML parser upon controllers' registrations.
	 * Can be modified statically before context initiation to use another prefix. */
	public static String SCREEN_TRANSITION_ACTION_PREFIX = "goto:";
	/** Length of views' fading in and out. Can be modified statically to change screen transition length,
	 * without having to set a different action provider with {@link #setHidingActionProvider(ActionProvider)}
	 * or {@link #setShowingActionProvider(ActionProvider)}. Retrieved each time a screen is shown or hidden
	 * using default actions. */
	public static float DEFAULT_FADING_TIME = 0.25f;

	private final ObjectMap<Class<?>, ViewController> controllers = GdxMaps.newObjectMap();
	private final ObjectMap<Class<?>, ViewDialogController> dialogControllers = GdxMaps.newObjectMap();
	private ObjectMap<String, FileHandle> i18nBundleFiles;

	private final Skin skin = new Skin();
	private final LmlParser parser = Lml.parser(skin).build();
	private final Batch batch = new SpriteBatch();

	private final ScreenSwitchingRunnable screenSwitchingRunnable = new ScreenSwitchingRunnable(this);
	private ViewController currentController;
	private Locale lastLocale;
	boolean isControllerHiding;

	@Inject
	private AssetService assetService;
	@Inject
	private LocaleService localeService;
	@Inject
	private MusicService musicService;
	@Inject
	private ViewActionContainerAnnotationProcessor viewActionProcessor;

	private ActionProvider showingActionProvider = getDefaultViewShowingActionProvider();
	private ActionProvider hidingActionProvider = getDefaultViewHidingActionProvider();
	private ObjectProvider<Viewport> viewportProvider = getDefaultViewportProvider();

	/** Registers {@link com.badlogic.gdx.Preferences} object to the LML parser.
	 *
	 * @param preferencesKey key of the preferences as it appears in LML views.
	 * @param preferencesPath path to the preferences. */
	public void addPreferencesToParser(final String preferencesKey, final String preferencesPath) {
		parser.setPreferences(preferencesKey, ApplicationPreferences.getPreferences(preferencesPath));
	}

	/** Registers an action container globally for all views.
	 *
	 * @param actionContainerId ID of the action container as it appears in the views.
	 * @param actionContainer contains view actions. */
	public void addViewActionContainer(final String actionContainerId, final ActionContainer actionContainer) {
		parser.addActionContainer(actionContainerId, actionContainer);
	}

	/** Registers an action globally for all views.
	 *
	 * @param actionId ID of the action.
	 * @param action will be available in views with the selected ID. */
	public void addViewAction(final String actionId, final ActorConsumer<?, ?> action) {
		parser.addAction(actionId, action);
	}

	@Initiate(priority = AutumnActionPriority.TOP_PRIORITY)
	private void assignViewResources(final SkinAnnotationProcessor skinProcessor,
			final I18nBundleAnnotationProcessor bundleProcessor) {
		initiateSkin(skinProcessor);
		i18nBundleFiles = bundleProcessor.getBundleFiles();
		buildParser();
	}

	private void initiateSkin(final SkinAnnotationProcessor skinProcessor) {
		final SkinData skinData = skinProcessor.getSkinData();
		final String atlasPath = skinData.getPath() + CommonFileExtension.ATLAS;
		assetService.load(atlasPath, TextureAtlas.class);
		final TextureAtlas skinAtlas = assetService.finishLoading(atlasPath, TextureAtlas.class);

		final String[] fontPaths = skinData.getFonts();
		loadFonts(atlasPath, fontPaths);
		skin.addRegions(skinAtlas);
		assignFonts(skinData, fontPaths);
		skin.load(Gdx.files.internal(skinData.getPath() + CommonFileExtension.JSON));
	}

	private void loadFonts(final String atlasPath, final String[] fontPaths) {
		if (fontPaths.length != 0) {
			final BitmapFontParameter loadingParameters = new BitmapFontParameter();
			loadingParameters.atlasName = atlasPath;
			for (final String fontPath : fontPaths) {
				assetService.load(fontPath, BitmapFont.class, loadingParameters);
			}
			assetService.finishLoading();
		}
	}

	private void assignFonts(final SkinData skinData, final String[] fontPaths) {
		if (fontPaths.length != 0) {
			final String[] fontNames = skinData.getFontsNames();
			for (int fontIndex = 0; fontIndex < fontPaths.length; fontIndex++) {
				skin.add(fontNames[fontIndex], assetService.get(fontPaths[fontIndex], BitmapFont.class),
						BitmapFont.class);
			}
		}
	}

	private void buildParser() {
		addDefaultViewActions();
		lastLocale = localeService.getCurrentLocale();
		for (final Entry<String, FileHandle> bundleData : i18nBundleFiles) {
			parser.setI18nBundle(bundleData.key, I18NBundle.createBundle(bundleData.value, lastLocale));
		}
	}

	private void addDefaultViewActions() {
		parser.addAction(ApplicationExitAction.ID, new ApplicationExitAction(this));
		parser.addAction(ApplicationPauseAction.ID, new ApplicationPauseAction());
		parser.addAction(ApplicationResumeAction.ID, new ApplicationResumeAction());
	}

	/** Allows to manually register a managed controller. For internal use mostly.
	 *
	 * @param mappedControllerClass class with which the controller is accessible. This does not have to be
	 *            controller's actual class.
	 * @param controller controller implementation, managing a single view. */
	public void registerController(final Class<?> mappedControllerClass, final ViewController controller) {
		controllers.put(mappedControllerClass, controller);
		if (Strings.isNotEmpty(controller.getId())) {
			parser.addAction(SCREEN_TRANSITION_ACTION_PREFIX + controller.getId(),
					new ScreenTransitionAction(this, mappedControllerClass));
		}
	}

	public void registerDialogController(final Class<?> mappedDialogControllerClass,
			final ViewDialogController dialogController) {
		dialogControllers.put(mappedDialogControllerClass, dialogController);
		if (Strings.isNotEmpty(dialogController.getId())) {
			parser.addAction(DIALOG_SHOWING_ACTION_PREFIX + dialogController.getId(),
					new DialogShowingAction(this, mappedDialogControllerClass));
		}
	}

	@Initiate(priority = AutumnActionPriority.MIN_PRIORITY)
	private void initiateFirstScreen() {
		for (final ViewController controller : controllers.values()) {
			if (controller.isFirst()) {
				show(controller);
				return;
			}
		}
		throw new AutumnRuntimeException("At least one view has to be set as first.");
	}

	private void initiateView(final ViewController controller) {
		if (!controller.isCreated()) {
			validateLocale();
			final ActionContainer actionContainer = controller.getActionContainer();
			registerViewSpecificActions(controller.getId(), actionContainer);
			controller.createView(this);
			unregisterViewSpecificActions(controller.getId(), actionContainer);
			parser.clearActorsMappedById();
		}
	}

	private void registerViewSpecificActions(final String controllerId, final ActionContainer actionContainer) {
		if (actionContainer != null) {
			parser.addActionContainer(controllerId, actionContainer);
		}
		final Array<ViewActionProvider> viewSpecificActions = viewActionProcessor.getActionProviders();
		if (GdxArrays.isNotEmpty(viewSpecificActions)) {
			for (final ViewActionProvider actionProvider : viewSpecificActions) {
				actionProvider.register(parser, controllerId);
			}
		}
	}

	private void unregisterViewSpecificActions(final String controllerId,
			final ActionContainer actionContainer) {
		if (actionContainer != null) {
			parser.removeActionContainer(controllerId);
		}
		final Array<ViewActionProvider> viewSpecificActions = viewActionProcessor.getActionProviders();
		if (GdxArrays.isNotEmpty(viewSpecificActions)) {
			for (final ViewActionProvider actionProvider : viewSpecificActions) {
				actionProvider.unregister(parser, controllerId);
			}
		}
	}

	private void validateLocale() {
		final Locale currentLocale = localeService.getCurrentLocale();
		if (!lastLocale.equals(currentLocale)) {
			lastLocale = currentLocale;
			for (final Entry<String, FileHandle> bundleData : i18nBundleFiles) {
				parser.setI18nBundle(bundleData.key, I18NBundle.createBundle(bundleData.value, currentLocale));
			}
		}
	}

	/** @return LML parser that should be used to construct views. */
	public LmlParser getParser() {
		return parser;
	}

	/** @return {@link com.badlogic.gdx.graphics.g2d.SpriteBatch} instance used to render all views. */
	public Batch getBatch() {
		return batch;
	}

	/** @return skin used to build views. */
	public Skin getSkin() {
		return skin;
	}

	/** Hides current view (if present) and shows the view managed by the passed controller.
	 *
	 * @param controller class of the controller managing the view. */
	public void show(final Class<?> controller) {
		show(controllers.get(controller));
	}

	/** Hides current view (if present) and shows the view managed by the chosen controller
	 *
	 * @param viewController will be set as the current view and shown. */
	public void show(final ViewController viewController) {
		if (currentController != null) {
			if (isControllerHiding) {
				switchToView(viewController);
			} else {
				hideCurrentViewAndSchedule(viewController);
			}
		} else {
			switchToView(viewController);
		}
	}

	/** Allows to show a globally registered dialog.
	 *
	 * @param dialogControllerClass class managing a single dialog. */
	public void showDialog(final Class<?> dialogControllerClass) {
		if (currentController != null) {
			dialogControllers.get(dialogControllerClass).show(currentController.getStage());
		}
	}

	private void switchToView(final ViewController viewController) {
		Gdx.app.postRunnable(screenSwitchingRunnable.switchToView(viewController));
	}

	private void hideCurrentViewAndSchedule(final ViewController viewController) {
		isControllerHiding = true;
		currentController.hide(Actions.sequence(
				hidingActionProvider.provideAction(currentController, viewController),
				Actions.run(CommonActionRunnables.getViewSetterRunnable(this, viewController))));
	}

	/** Forces eager initiation of all views managed by registered controllers. */
	public void initiateAllControllers() {
		for (final ViewController controller : controllers.values()) {
			initiateView(controller);
		}
	}

	/** @return provider of viewports that should be used to construct stages. */
	public ObjectProvider<Viewport> getViewportProvider() {
		return viewportProvider;
	}

	/** @param viewportProvider used to construct stages. */
	public void setViewportProvider(final ObjectProvider<Viewport> viewportProvider) {
		this.viewportProvider = viewportProvider;
	}

	/** @param hidingActionProvider used to provide default actions that hide views. Connected view of the
	 *            provider will be the next view shown after this one and might be null. */
	public void setHidingActionProvider(final ActionProvider hidingActionProvider) {
		this.hidingActionProvider = hidingActionProvider;
	}

	/** @param showingActionProvider used to provide default actions that show views. Should set input
	 *            processor. Connected view of the provider will be the previous view shown before this one
	 *            and might be null. */
	public void setShowingActionProvider(final ActionProvider showingActionProvider) {
		this.showingActionProvider = showingActionProvider;
	}

	/** Hides current view, destroys all screens and shows the recreated current view. Note that it won't
	 * recreate all views that were previously initiated, as views are constructed on demand.
	 *
	 * @see #initiateAllControllers() */
	public void reload() {
		currentController.hide(Actions.sequence(
				hidingActionProvider.provideAction(currentController, currentController),
				Actions.run(CommonActionRunnables.getActionPosterRunnable(getViewReloadingRunnable()))));
	}

	/** Forces destruction of the selected view. It should not be currently shown.
	 *
	 * @param viewController will be destroyed. */
	public void destroy(final Class<?> viewController) {
		controllers.get(viewController).destroyView();
	}

	private Runnable getViewReloadingRunnable() {
		return new Runnable() {
			@Override
			public void run() {
				reloadViews();
			}
		};
	}

	private void reloadViews() {
		destroyViews();
		final ViewController viewToShow = currentController;
		currentController = null;
		show(viewToShow);
	}

	/** Renders the current view, if present.
	 *
	 * @param delta time passed since the last update. */
	public void render(final float delta) {
		if (currentController != null) {
			currentController.render(delta);
		}
	}

	/** Resizes the current view, if present.
	 *
	 * @param width new width of the screen.
	 * @param height new height of the screen. */
	public void resize(final int width, final int height) {
		if (currentController != null) {
			currentController.resize(width, height);
		}
	}

	/** Pauses the current view, if present. */
	public void pause() {
		if (currentController != null) {
			currentController.pause();
		}
	}

	/** Resumes the current view, if present. */
	public void resume() {
		if (currentController != null) {
			currentController.resume();
		}
	}

	/** @return controller of currently shown view. Might be null. Mostly for internal use. */
	public ViewController getCurrentController() {
		return currentController;
	}

	/** @param forClass class associated with the controller. Does not have to be a
	 *            {@link com.github.czyzby.autumn.mvc.component.ui.controller.ViewController} - can be a
	 *            wrapped by an auto-generated controller instance.
	 * @return instance of the passed class or a controller wrapping the selected class. Can be null. */
	public ViewController getController(final Class<?> forClass) {
		return controllers.get(forClass);
	}

	/** @param forClass class associated with the dialog controller. Does not have to be a
	 *            {@link com.github.czyzby.autumn.mvc.component.ui.controller.ViewDialogController} - can be a
	 *            wrapped by an auto-generated controller instance.
	 * @return instance of the passed class or a dialog controller wrapping the selected class. Can be null. */
	public ViewDialogController getDialogController(final Class<?> forClass) {
		return dialogControllers.get(forClass);
	}

	@Destroy(priority = AutumnActionPriority.LOW_PRIORITY)
	private void dispose() {
		destroyViews();
		controllers.clear();
		batch.dispose();
		skin.dispose();
	}

	private void destroyViews() {
		for (final ViewController controller : controllers.values()) {
			controller.destroyView();
		}
	}

	/** Allows to smoothly close the application by hiding the current screen and calling
	 * {@link com.badlogic.gdx.Application#exit()}. */
	public void exitApplication() {
		if (currentController != null) {
			currentController.hide(Actions.sequence(
					hidingActionProvider.provideAction(currentController, null),
					Actions.run(CommonActionRunnables.getApplicationClosingRunnable())));
		} else {
			Gdx.app.exit();
		}
	}

	private ActionProvider getDefaultViewShowingActionProvider() {
		return new ActionProvider() {
			@Override
			public Action provideAction(final ViewController forController,
					final ViewController previousController) {
				if (musicService.getCurrentTheme() == null && GdxArrays.isNotEmpty(forController.getThemes())) {
					final Music currentTheme = forController.getThemes().random();
					return Actions.sequence(Actions.alpha(0f), Actions.fadeIn(DEFAULT_FADING_TIME), Actions
							.run(CommonActionRunnables
									.getMusicThemeSetterRunnable(musicService, currentTheme)), Actions
							.run(CommonActionRunnables.getInputSetterRunnable(forController.getStage())),
							MusicFadingAction.fadeIn(currentTheme, MusicService.DEFAULT_THEME_FADING_TIME,
									musicService.getMusicVolume()));
				}
				return Actions.sequence(Actions.alpha(0f), Actions.fadeIn(DEFAULT_FADING_TIME),
						Actions.run(CommonActionRunnables.getInputSetterRunnable(forController.getStage())));
			}
		};
	}

	private ActionProvider getDefaultViewHidingActionProvider() {
		return new ActionProvider() {
			@Override
			public Action provideAction(final ViewController forController,
					final ViewController nextController) {
				final Music currentTheme = musicService.getCurrentTheme();
				if (currentTheme == null || isThemeAvailableInNextView(nextController, currentTheme)) {
					return Actions.sequence(Actions.run(CommonActionRunnables.getInputClearerRunnable()),
							Actions.fadeOut(DEFAULT_FADING_TIME));
				}
				return Actions.sequence(Actions.run(CommonActionRunnables.getInputClearerRunnable()), Actions
						.parallel(MusicFadingAction.fadeOut(currentTheme,
								MusicService.DEFAULT_THEME_FADING_TIME), Actions.sequence(
								Actions.delay(MusicService.DEFAULT_THEME_FADING_TIME - DEFAULT_FADING_TIME),
								Actions.fadeOut(DEFAULT_FADING_TIME))), Actions.run(CommonActionRunnables
						.getMusicThemeClearerRunnable(musicService)));
			}

			private boolean isThemeAvailableInNextView(final ViewController nextController,
					final Music currentTheme) {
				return nextController != null && currentTheme != null
						&& GdxArrays.isNotEmpty(nextController.getThemes())
						&& nextController.getThemes().contains(currentTheme, false);
			}
		};
	}

	private ObjectProvider<Viewport> getDefaultViewportProvider() {
		return new ObjectProvider<Viewport>() {
			@Override
			public Viewport provide() {
				return new ScreenViewport();
			}
		};
	}

	/** Avoids anonymous classes.
	 *
	 * @author MJ */
	private static class ScreenSwitchingRunnable implements Runnable {
		private final InterfaceService interfaceService;
		private ViewController controllerToShow;

		public ScreenSwitchingRunnable(final InterfaceService interfaceService) {
			this.interfaceService = interfaceService;
		}

		public Runnable switchToView(final ViewController controllerToShow) {
			this.controllerToShow = controllerToShow;
			return this;
		}

		@Override
		public void run() {
			interfaceService.isControllerHiding = false;
			final ViewController previousController = interfaceService.currentController;
			interfaceService.currentController = controllerToShow;
			interfaceService.initiateView(controllerToShow);
			interfaceService.currentController.show(interfaceService.showingActionProvider.provideAction(
					interfaceService.currentController, previousController));
			controllerToShow = null;
		}
	}
}