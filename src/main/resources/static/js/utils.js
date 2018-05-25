app.factory("utils", function ($http, $location, $window) {

    this.updateAddressBarPathByParams = function (params) {
        $location.path(GAME_PREFIX + params.game.id + POSITION_PREFIX + params.game.position);
    };

    return this;
});







