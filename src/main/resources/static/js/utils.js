app.factory("utils", function ($http, $location, $window) {

    this.updateAddressBarPathByParams = function (params) {
        $location.path(GAME_PREFIX + params.game.id + POSITION_PREFIX + params.game.position);
    };

    this.getCurrentUrl = function () {
        return $location.path();
    };

    this.redirectTo = function (href) {
        $window.location.href = href;
    };

    this.redirectToIndex = function () {
        redirectTo("/");
    };

    return this;
});







