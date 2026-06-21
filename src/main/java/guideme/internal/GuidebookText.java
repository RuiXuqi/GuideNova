package guideme.internal;

import guideme.internal.data.LocalizationEnum;

public enum GuidebookText implements LocalizationEnum {
    HistoryGoBack,
    HistoryGoForward,
    Close,
    HoldToShow,
    HideAnnotations,
    ShowAnnotations,
    ZoomIn,
    ZoomOut,
    ResetView,
    Search,
    SearchNoQuery,
    SearchNoResults,
    ContentFrom,
    ItemNoGuideId,
    ItemInvalidGuideId,
    CommandOnlyWorksInSinglePlayer,
    Smelting,
    Blasting,
    ShapelessCrafting,
    Crafting,
    FullWidthView,
    CloseFullWidthView,
    RunsCommand;

    @Override
    public String getTranslationKey() {
        return "guideme.guidebook." + name();
    }
}
