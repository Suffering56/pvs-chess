/** @namespace selectedCell.piece */
app.controller("initController", function ($rootScope, $scope, $http, utils) {
    var params = $rootScope.params;
    var path = utils.getCurrentUrl();

    initializeGame();   //starting new game or continue already started game
    $scope.sideClick = sideClick;
    $scope.modeClick = modeClick;
    $scope.isShowModePanel = isShowModePanel;
    $scope.isShowSidePanel = isShowSidePanel;


    function isShowModePanel() {
        return params.mode == MODE_UNSELECTED && params.gameStarted == false;
    }

    function isShowSidePanel() {
        return params.mode != MODE_UNSELECTED && params.side == SIDE_UNSELECTED && params.gameStarted == false;
    }

    function modeClick(mode) {
        $http({
            method: "POST",
            url: "/api/init/" + params.game.id + "/mode",
            data: {
                mode: mode
            }
        }).then(function () {
            params.mode = mode;
        });
    }

    function sideClick(side) {
        if (side == SIDE_VIEWER) {
            params.side = side;
            updateArrangement();
            return;
        }
        
        $http({
            method: "POST",
            url: "/api/init/" + params.game.id + "/side",
            data: {
                side: side
            }
        }).then(function () {
            params.side = side;
            updateArrangement();
        });
    }

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
                params.mode = game.mode;

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
            var sideDTO = response.data;
            params.side = sideDTO.side;

            if (params.side == SIDE_UNSELECTED) {
                return;
            }

            if (params.side == SIDE_VIEWER) {
                alert("all gaming places are occupied - you can only view this game");
            }

            updateArrangement();
        });
    }

});