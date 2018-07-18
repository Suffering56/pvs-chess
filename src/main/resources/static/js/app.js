var app = angular.module("app", ["ui.bootstrap", "dialogs.main"]);

const GAME_PREFIX = "/game/";
const POSITION_PREFIX = "/position/";

app.run(['$rootScope', function ($rootScope) {

    $rootScope.cellsMatrix = null;
    $rootScope.params = {
        gameStarted: false,
        isWhite: null,      //null = unselected, true = white, false = black
        isViewer: false,    //if true = disable moves
        game: {
            id: null,
            position: null,
            underCheckSide: null
        }
    };
}]);

app.config(["$locationProvider", function ($locationProvider) {
    $locationProvider.html5Mode({
        enabled: true,
        requireBase: false
    });
}]);

app.filter("chessboardReverse", function () {
    return function (items, isWhite) {
        if (isWhite) {
            return items.slice().reverse();
        } else {
            return items;
        }
    };
});
