/** @namespace selectedCell.piece */
app.controller("playgroundController", function ($rootScope, $scope, $http, utils, dialogs) {
    $scope.horizontalLabels = ["h", "g", "f", "e", "d", "c", "b", "a"];
    $scope.verticalLabels = ["1", "2", "3", "4", "5", "6", "7", "8"];

    $scope.doClick = doClick;
    $scope.getCellColorClass = getCellColorClass;
    $scope.getCellPieceClass = getCellPieceClass;
    $scope.rollback = rollback;
    $scope.wakeBot = wakeBot;

    let params = $rootScope.params;
    let game = params.game;

    let selectedCell;
    let availablePoints;

    if (params.mode === MODE_AI || params.mode === MODE_PVP) {
        utils.createAndStartTimer(function () {
            listenEnemyActions();
        }, 500);
    }
    // utils.stopTimer(timer);

    function listenEnemyActions() {
        $http({
            method: "GET",
            url: "/api/game/" + game.id + "/listen"
        }).then(function (response) {
            let arrangementDTO = response.data;
            if (arrangementDTO.position !== params.game.position) {
                updateArrangement(arrangementDTO);
            }
        });
    }

    function doClick(cell) {
        //cell = CellDTO
        if (params.side === SIDE_VIEWER || cell.selected === true) {
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
        if (selectedCell.piece.type === PIECE_PAWN) {
            if (cell.rowIndex === 0 || cell.rowIndex === 7) {
                showPieceChooser(cell);
                return;
            }
        }

        sendApplyMoveRequest(cell);
    }

    function showPieceChooser(cell) {
        let data = {side: selectedCell.piece.side};
        let dlg = dialogs.create("/modal/piece-chooser.html", "pieceChooserController", data, {size: "md"});

        dlg.result.then(function (pieceType) {
            //success
            sendApplyMoveRequest(cell, pieceType);
        }, function () {
            //canceled = do nothing
        });
    }

    function sendApplyMoveRequest(cell, selectedPieceType) {
        let url = "/api/game/" + game.id + "/move";

        let moveDTO = {
            from: {
                rowIndex: selectedCell.rowIndex,
                columnIndex: selectedCell.columnIndex
            },
            to: {
                rowIndex: cell.rowIndex,
                columnIndex: cell.columnIndex
            },
            pieceFromPawn: null
        };

        if (selectedPieceType) {
            moveDTO.pieceFromPawn = selectedPieceType;
        }

        $http({
            method: "POST",
            url: url,
            data: moveDTO
        }).then(function (response) {
            updateArrangement(response.data);
        }, function (reason) {
            console.log(reason);
            alert(reason);
        });
    }

    function updateArrangement(arrangementDTO) {
        game.position = arrangementDTO.position;
        $rootScope.cellsMatrix = arrangementDTO.cellsMatrix;
        game.underCheckSide = arrangementDTO.underCheckSide;

        utils.updateAddressBarPathByParams(params);
    }

    function selectCell(cell) {
        if (params.mode !== MODE_SINGLE) {
            let expectedSide = getExpectedSide();

            if (cell.piece == null || cell.piece.side !== expectedSide) {
                return;
            }
            if (params.side !== expectedSide) {
                return;
            }
        }

        //cell = cellDTO
        clearAvailablePoints();

        if (selectedCell) {
            selectedCell.selected = false;
        }

        if (cell.piece != null && cell.piece.side === getExpectedSide()) {
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

    function getCellColorClass(cell) {
        //cell = cellDTO
        if ((cell.rowIndex + cell.columnIndex) % 2 === 0) {
            return "white";
        } else {
            return "black";
        }
    }

    function getCellPieceClass(cell) {
        //cell = cellDTO
        let result = [];

        if (cell.piece) {
            result.push("piece");
            result.push(cell.piece.type + "-" + cell.piece.side);
            if (cell.piece.type === PIECE_KING) {
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
        return (game.position % 2 === 0) ? SIDE_WHITE : SIDE_BLACK;
    }

    function getEnemySide() {
        return (game.position % 2 === 0) ? SIDE_BLACK : SIDE_WHITE;
    }

    function rollback() {
        $http({
            method: "GET",
            url: "/api/game/" + game.id + "/rollback"
        }).then(function (response) {
            updateArrangement(response.data);
        });
    }

    function wakeBot() {
        $http({
            method: "GET",
            url: "/api/game/" + game.id + "/wake"
        }).then(function (response) {

        });
    }

});