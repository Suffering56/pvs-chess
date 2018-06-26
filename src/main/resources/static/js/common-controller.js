app.controller("common", function ($scope, $http, $window, initService, utils, dialogs) {
    $scope.horizontalLabels = ["h", "g", "f", "e", "d", "c", "b", "a"];
    $scope.verticalLabels = ["1", "2", "3", "4", "5", "6", "7", "8"];
    $scope.params = {
        gameStarted: false,
        isWhite: null,      //null = unselected, true = white, false = black
        isViewer: false,    //if true = disable moves
        game: {
            id: null,
            position: null,
            underCheckSide: null
        }
    };

    var params = $scope.params;
    var game = params.game;

    //initialize game (starting new game or continue already started game)
    initService.init(params, updateArrangement);
    //handle the click of side selection button
    $scope.sideClick = initService.sideClick;

    function updateArrangement() {
        utils.updateAddressBarPathByParams(params);

        $http({
            method: "GET",
            url: "/api/init/" + params.game.id + "/arrangement/" + params.game.position
        }).then(function (response) {
            var arrangementDTO = response.data;
            $scope.cellsMatrix = arrangementDTO.cellsMatrix;
            params.game.underCheckSide = arrangementDTO.underCheckSide;
            params.gameStarted = true;
        });
    }

    var selectedCell;
    var availablePoints;

    $scope.doClick = function (cell) {
        if (params.isViewer === true || cell.selected === true) {
            return;
        }

        if (cell.available === true) {
            applyMove(cell);
        } else {
            selectCell(cell);
        }
    };

    function applyMove(cell) {
        // if (cell.columnIndex == 0) {
        //     dialogs.confirm('Please Confirm', 'confirm');
        //     return;
        // }

        var url = "/api/game/" + game.id + "/move";
        $http({
            method: "POST",
            url: url,
            data: {
                from: {
                    rowIndex: selectedCell.rowIndex,
                    columnIndex: selectedCell.columnIndex
                },
                to: {
                    rowIndex: cell.rowIndex,
                    columnIndex: cell.columnIndex
                }
            }
        }).then(function (response) {
            var arrangementDTO = response.data;

            game.position = arrangementDTO.position;
            $scope.cellsMatrix = arrangementDTO.cellsMatrix;
            game.underCheckSide = arrangementDTO.underCheckSide;

            utils.updateAddressBarPathByParams(params);

        }, function (reason) {
            console.log(reason);
            alert(reason);
        });
    }

    function selectCell(cell) {

        clearAvailablePoints();
        if (selectedCell) {
            selectedCell.selected = false;
        }

        if (cell.piece != null && cell.piece.side && cell.piece.side === getExpectedSide()) {
            cell.selected = true;
            selectedCell = cell;

            getAvailableMoves(cell);
        }
    }

    function getAvailableMoves(cell) {
        $http({
            method: "GET",
            url: "/api/game/" + game.id + "/move",
            params: {
                rowIndex: cell.rowIndex,
                columnIndex: cell.columnIndex
            }
        }).then(function (response) {
            handleAvailableMoves(response.data);
        });
    }

    var handleAvailableMoves = function (points) {
        points.map(function (point) {
            getCellByPoint(point).available = true;
        });
        availablePoints = points;
    };

    function clearAvailablePoints() {
        if (availablePoints) {
            availablePoints.map(function (point) {
                getCellByPoint(point).available = false;
            });
        }
    }

    $scope.getCellClass = function (cell) {
        if ((cell.rowIndex + cell.columnIndex) % 2 === 0) {
            return "white";
        } else {
            return "black";
        }
    };

    $scope.getInnerCellClass = function (cell) {
        var result = [];

        if (cell.piece) {
            result.push("piece");
            result.push(cell.piece.type + "-" + cell.piece.side);
            if (cell.piece.type === "king") {
                if (params.game.underCheckSide  === cell.piece.side) {
                    result.push("check");
                }
            }
        }

        if (cell.selected === true) {
            result.push('selected');
        } else if (cell.available === true) {
            result.push('available');
            if (cell.piece != null && cell.piece.side === getEnemySide()){
                result.push('capture');
            }
        }

        return result;
    };

    function getCell(rowIndex, columnIndex) {
        return $scope.cellsMatrix[rowIndex][columnIndex];
    }

    function getCellByPoint(point) {
        return getCell(point.rowIndex, point.columnIndex);
    }

    function getExpectedSide() {
        return (game.position % 2 === 0) ? "white" : "black";
    }

    function getEnemySide() {
        return (game.position % 2 === 0) ? "black" : "white";
    }

});