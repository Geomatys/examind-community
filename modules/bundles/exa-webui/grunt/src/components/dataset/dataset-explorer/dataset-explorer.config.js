angular.module("examind.components.dataset.explorer")
    .config(datasetExplorerConfiguration);

function datasetExplorerConfiguration($translatePartialLoaderProvider) {
    $translatePartialLoaderProvider.addPart('dataset-explorer');
}