/** @namespace selectedCell.piece */
app.controller("initController", function ($rootScope, $scope, $http, utils) {
    var params = $rootScope.params;
    var path = utils.getCurrentUrl();

    initializeGame();   //starting new game or continue already started game
    $scope.sideClick = sideClick;

    function initializeGame() {
        if (path.indexOf(GAME_PREFIX) !== -1) {
            var pathParts = path.split("/");
            params.game.id = pathParts[2];

            if (path.indexOf(POSITION_PREFIX) !== -1) {
                params.game.position = pathParts[4];
            }
        }

        if (params.game.id == null) {
            createGame();
        } else {
            continueGame();
        }
    }

    function updateArrangement() {
        utils.updateAddressBarPathByParams(params);

        $http({
            method: "GET",
            url: "/api/init/" + params.game.id + "/arrangement/" + params.game.position
        }).then(function (response) {

            var arrangementDTO = response.data;
            $rootScope.cellsMatrix = arrangementDTO.cellsMatrix;
            params.game.underCheckSide = arrangementDTO.underCheckSide;
            params.gameStarted = true;
        });
    }

    function sideClick(isWhite) {
        $http({
            method: "POST",
            url: "/api/init/" + params.game.id + "/side",
            data: {
                isWhite: isWhite
            }
        }).then(function () {
            params.isWhite = isWhite;
            call(updateArrangement());
        });
    }

    function createGame() {
        $http({
            method: "GET",
            url: "/api/init"
        }).then(function (response) {
            var game = response.data;
            params.game.id = game.id;
            params.game.position = game.position;
        });
    }

    function continueGame() {
        $http({
            method: "GET",
            url: "/api/init/" + params.game.id
        }).then(function (response) {
            var game = response.data;
            if (!game) {
                alert("game not found. Starting a new game...");
                utils.redirectToIndex();
            } else {
                params.game.id = game.id;

                if (params.game.position) {
                    if (params.game.position > game.position) {
                        params.game.position = game.position;
                    }
                } else {
                    params.game.position = game.position;
                }

                checkPlayerSide();
            }
        });
    }

    function checkPlayerSide() {
        $http({
            method: "GET",
            url: "/api/init/" + params.game.id + "/side"
        }).then(function (response) {
            var paramsPlayerDTO = response.data;

            params.isWhite = paramsPlayerDTO.isWhite;
            params.isViewer = paramsPlayerDTO.isViewer;

            if (paramsPlayerDTO.isViewer === true) {
                alert("all gaming places are occupied - you can only view this game");
                call(updateArrangement());
            } else if (paramsPlayerDTO.isWhite != null) {
                call(updateArrangement());
            }
        });
    }

    function call(callback) {
        if (callback) {
            callback();
        }
    }
});