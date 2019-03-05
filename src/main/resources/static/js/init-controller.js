/** @namespace selectedCell.piece */
app.controller("initController", function ($rootScope, $scope, $http, utils, $location) {
    let params = $rootScope.params;
    let path = utils.getCurrentUrl();

    $scope.sideClick = sideClick;
    $scope.modeClick = modeClick;
    $scope.isShowModePanel = isShowModePanel;
    $scope.isShowSidePanel = isShowSidePanel;

    initializeGame();                                           //starting new game or continue already started game:

    // New game:                                                from:   /
    //  - Create new game                                       GET     /api/init
    //  - UI: Choose mode (AI/PVP/SINGLE)                       POST    /api/init/{gameId}/mode
    //  - UI: Choose side (WHITE/BLACK/VIEWER)                  POST    /api/init/{gameId}/side
    //  - Update arrangement                                    GET     /api/init/game/{gameId}/arrangement/0

    // Continue game:                                           from:   /game/{gameId}/position/{position}
    //  - Get game                                              GET     /api/init/game/{gameId}
    //  - Get side                                              GET     /api/init/game/{gameId}/side
    //  - Update arrangement                                    GET     /api/init/game/{gameId}/arrangement/{position}

    // Debug game:                                              /game/{gameId}/position/{position}?debug=true&side=desiredSide
    //  - Get game                                              GET     /api/init/game/{gameId}
    //  - Get side                                              GET     /api/init/game/{gameId}/side


    function isShowModePanel() {
        return params.mode === MODE_UNSELECTED && params.gameStarted === false;
    }

    function isShowSidePanel() {
        return params.mode !== MODE_UNSELECTED && params.side === SIDE_UNSELECTED && params.gameStarted === false;
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
        if (side === SIDE_VIEWER) {
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
            let pathParts = path.split("/");

            //FIXME: не очень гибко - мне кажется если корневой путь будет содержать слеши, то все сломается
            params.game.id = pathParts[2];

            if (path.indexOf(POSITION_PREFIX) !== -1) {
                //FIXME: не очень гибко - мне кажется если корневой путь будет содержать слеши, то все сломается
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

            let arrangementDTO = response.data;
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
            let game = response.data;
            params.game.id = game.id;
            params.game.position = game.position;
        });
    }

    function continueGame() {
        let url = "/api/init/" + params.game.id;
        let postfix = getDebugModeQueryParamsPostfix();
        if (postfix != null) {
            url += postfix;
        }

        $http({
            method: "GET",
            url: url
        }).then(function (response) {
            let game = response.data;
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

    function getDebugModeQueryParamsPostfix() {
        let debug = $location.search().debug;
        if (debug) {
            // noinspection JSUnresolvedVariable
            let desiredSide = $location.search().desiredSide;
            if (desiredSide) {
                if (debug === true) {
                   params.isDebug = true;
                }
                return "?debug=" + debug + "&desiredSide=" + desiredSide;
            } else {
                alert("Ты приходишь ко мне и просишь доступ в DEBUG_MODE, но делаешь это без уважения (не указав desiredSide)...")
            }
        }
        return null;
    }

    function checkPlayerSide() {
        $http({
            method: "GET",
            url: "/api/init/" + params.game.id + "/side"
        }).then(function (response) {
            let sideDTO = response.data;
            params.side = sideDTO.side;

            if (params.side === SIDE_UNSELECTED) {
                return;
            }

            if (params.side === SIDE_VIEWER) {
                alert("all gaming places are occupied - you can only view this game");
            }

            updateArrangement();
        });
    }

});