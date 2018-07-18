/** @namespace selectedCell.piece */
app.controller("playgroundController", function ($rootScope, $scope, $http, utils, dialogs) {
    $scope.horizontalLabels = ["h", "g", "f", "e", "d", "c", "b", "a"];
    $scope.verticalLabels = ["1", "2", "3", "4", "5", "6", "7", "8"];

    $scope.doClick = doClick;
    $scope.getCellClass = getCellClass;
    $scope.getInnerCellClass = getInnerCellClass;

    var params = $rootScope.params;
    var game = params.game;

    var selectedCell;
    var availablePoints;


    function doClick(cell) {
        //cell = CellDTO
        if (params.isViewer === true || cell.selected === true) {
            return;
        }

        if (cell.available === true) {
            applyMove(cell);
        } else {
            selectCell(cell);
        }
    }

    function applyMove(cell) {
        //cell = CellDTO
        if (selectedCell.piece.type == "pawn") {
            if (cell.rowIndex == 0 || cell.rowIndex == 7) {
                showPieceChooser(cell);
                return;
            }
        }

        sendApplyMoveRequest(cell);
    }

    function showPieceChooser(cell) {
        var data = {side: selectedCell.piece.side};
        var dlg = dialogs.create("/modal/piece-chooser.html", "pieceChooserController", data, {size: "md"});

        dlg.result.then(function (pieceType) {
            //success
            sendApplyMoveRequest(cell, pieceType);
        }, function () {
            //canceled = do nothing
        });
    }

    function sendApplyMoveRequest(cell, selectedPieceType) {
        var url = "/api/game/" + game.id + "/move";

        var moveDTO = {
            from: {
                rowIndex: selectedCell.rowIndex,
                columnIndex: selectedCell.columnIndex
            },
            to: {
                rowIndex: cell.rowIndex,
                columnIndex: cell.columnIndex
            },
            pieceType: null
        };

        if (selectedPieceType) {
            moveDTO.pieceType = selectedPieceType;
        }

        $http({
            method: "POST",
            url: url,
            data: moveDTO
        }).then(function (response) {
            var arrangementDTO = response.data;

            game.position = arrangementDTO.position;
            $rootScope.cellsMatrix = arrangementDTO.cellsMatrix;
            game.underCheckSide = arrangementDTO.underCheckSide;

            utils.updateAddressBarPathByParams(params);

        }, function (reason) {
            console.log(reason);
            alert(reason);
        });
    }

    function selectCell(cell) {
        //cell = cellDTO
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
            //response.data = Set<PointDTO>
            handleAvailableMoves(response.data);
        });
    }

    function handleAvailableMoves(points) {
        //points = Set<PointDTO>
        points.map(function (point) {
            getCellByPoint(point).available = true;
        });
        availablePoints = points;
    }

    function clearAvailablePoints() {
        if (availablePoints) {
            availablePoints.map(function (point) {
                getCellByPoint(point).available = false;
            });
        }
    }

    function getCellClass(cell) {
        //cell = cellDTO
        if ((cell.rowIndex + cell.columnIndex) % 2 === 0) {
            return "white";
        } else {
            return "black";
        }
    }

    function getInnerCellClass(cell) {
        //cell = cellDTO
        var result = [];

        if (cell.piece) {
            result.push("piece");
            result.push(cell.piece.type + "-" + cell.piece.side);
            if (cell.piece.type === "king") {
                if (params.game.underCheckSide === cell.piece.side) {
                    result.push("check");
                }
            }
        }

        if (cell.selected === true) {
            result.push("selected");
        } else if (cell.available === true) {
            result.push("available");
            if (cell.piece != null && cell.piece.side === getEnemySide()) {
                result.push("capture");
            }
        }

        return result;
    }

    function getCell(rowIndex, columnIndex) {
        return $rootScope.cellsMatrix[rowIndex][columnIndex];
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