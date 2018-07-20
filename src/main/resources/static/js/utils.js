app.factory("utils", function ($http, $location, $window, $interval) {

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

    this.createAndStartTimer = function(callback, interval) {
        if (callback) {
            return $interval(function () {
                callback()
            }, interval);
        }
    };

    this.stopTimer = function(timer) {
        $interval.cancel(timer);
        timer = null;
    };

    return this;
});







