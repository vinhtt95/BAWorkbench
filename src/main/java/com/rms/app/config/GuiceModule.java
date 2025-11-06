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
        bind(IProjectStateService.class).to(ProjectStateServiceImpl.class).in(Singleton.class);

        bind(MainViewModel.class).in(Singleton.class);

        bind(FlowBuilderControl.class);

        bind(IProjectService.class).to(ProjectServiceImpl.class).in(Singleton.class);
        bind(ITemplateService.class).to(TemplateServiceImpl.class).in(Singleton.class);
        bind(IViewManager.class).to(ViewManagerImpl.class).in(Singleton.class);
        bind(IRenderService.class).to(RenderServiceImpl.class).in(Singleton.class);
        bind(ISearchService.class).to(SearchServiceImpl.class).in(Singleton.class);

        bind(IIndexService.class).to(IndexServiceImpl.class).in(Singleton.class);

        bind(IDiagramRenderService.class).to(DiagramRenderServiceImpl.class).in(Singleton.class);


        bind(IArtifactRepository.class).to(JsonFileRepository.class).in(Singleton.class);
        bind(ISqliteIndexRepository.class).to(SqliteIndexRepository.class).in(Singleton.class);
    }
}