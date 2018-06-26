var app = angular.module("app", ["ui.bootstrap", "dialogs.main"]);

const GAME_PREFIX = "/game/";
const POSITION_PREFIX = "/position/";

app.config(["$locationProvider", function ($locationProvider) {
    $locationProvider.html5Mode({
        enabled: true,
        requireBase: false
    });
}]);

app.filter('chessboardReverse', function () {
    return function (items, isWhite) {
        if (isWhite) {
            return items.slice().reverse();
        } else {
            return items;
        }
    };
});
