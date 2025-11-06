package com.rms.app.config;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.rms.app.repository.JsonFileRepository;
import com.rms.app.repository.SqliteIndexRepository;
import com.rms.app.service.*;
import com.rms.app.service.impl.*;
import com.rms.app.viewmodel.MainViewModel;
import com.rms.app.view.FlowBuilderControl;

public class GuiceModule extends AbstractModule {
    @Override
    protected void configure() {
        // Bind State (NEW)
        bind(IProjectStateService.class).to(ProjectStateServiceImpl.class).in(Singleton.class);

        // ViewModels
        bind(MainViewModel.class).in(Singleton.class);

        // Views/Controls (Cho FXML)
        bind(FlowBuilderControl.class);

        // Services (DIP)
        bind(IProjectService.class).to(ProjectServiceImpl.class).in(Singleton.class);
        bind(ITemplateService.class).to(TemplateServiceImpl.class).in(Singleton.class);
        bind(IViewManager.class).to(ViewManagerImpl.class).in(Singleton.class);
        bind(IRenderService.class).to(RenderServiceImpl.class).in(Singleton.class);
        bind(ISearchService.class).to(SearchServiceImpl.class).in(Singleton.class);

        // Repositories (DIP)
        bind(IArtifactRepository.class).to(JsonFileRepository.class).in(Singleton.class);

        bind(ISqliteIndexRepository.class).to(SqliteIndexRepository.class).in(Singleton.class);
    }
}