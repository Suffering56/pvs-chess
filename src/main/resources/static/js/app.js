const app = angular.module("app", ["ui.bootstrap", "dialogs.main"]);

const GAME_PREFIX = "/game/";
const POSITION_PREFIX = "/position/";

const MODE_UNSELECTED = null;
const MODE_PVP = "PVP";
const MODE_AI = "AI";
const MODE_SINGLE = "SINGLE";

const SIDE_UNSELECTED = null;
const SIDE_WHITE = "WHITE";
const SIDE_BLACK = "BLACK";
const SIDE_VIEWER = "VIEWER";

const PIECE_PAWN = "PAWN";
const PIECE_KNIGHT = "KNIGHT";
const PIECE_BISHOP = "BISHOP";
const PIECE_ROOK = "ROOK";
const PIECE_QUEEN = "QUEEN";
const PIECE_KING = "KING";

app.run(['$rootScope', function ($rootScope) {

    $rootScope.cellsMatrix = null;
    $rootScope.params = {
        mode: MODE_UNSELECTED,         //null = unselected, PVP, AI, HIMSELF
        gameStarted: false,
        side: SIDE_UNSELECTED,         //WHITE, BLACK, VIEWER, null (unselected)
        game: {
            id: null,
            position: null,
            underCheckSide: null
        },
        isDebug: false
    };

    $rootScope.MODE_UNSELECTED = MODE_UNSELECTED;
    $rootScope.MODE_PVP = MODE_PVP;
    $rootScope.MODE_AI = MODE_AI;
    $rootScope.MODE_SINGLE = MODE_SINGLE;

    $rootScope.SIDE_UNSELECTED = SIDE_UNSELECTED;
    $rootScope.SIDE_WHITE = SIDE_WHITE;
    $rootScope.SIDE_BLACK = SIDE_BLACK;
    $rootScope.SIDE_VIEWER = SIDE_VIEWER;
}]);

app.config(["$locationProvider", function ($locationProvider) {
    $locationProvider.html5Mode({
        enabled: true,
        requireBase: false
    });
}]);

app.filter("chessboardReverse", function () {
    return function (items, side) {
        if (side === SIDE_BLACK) {
            return items;
        }

        return items.slice().reverse();
    };
});
