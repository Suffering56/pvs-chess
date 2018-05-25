app.factory("initService", function ($http, $location, $window) {

    var path = $location.path();
    var params;
    var onStartGame;

    this.init = function (_params, _onStartGame) {
        onStartGame = _onStartGame;
        params = _params;

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
    };

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
                redirectToIndex();
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
                call(onStartGame());
            } else if (paramsPlayerDTO.isWhite != null) {
                call(onStartGame());
            }
        });
    }

    this.sideClick = function (isWhite) {
        $http({
            method: "POST",
            url: "/api/init/" + params.game.id + "/side",
            data: {
                isWhite: isWhite
            }
        }).then(function () {
            params.isWhite = isWhite;
            call(onStartGame());
        });
    };

    this.redirectTo = function (href) {
        $window.location.href = href;
    };

    this.redirectToIndex = function () {
        redirectTo("/");
    };

    function call(callback) {
        if (callback) {
            callback();
        }
    }

    return this;
})
;