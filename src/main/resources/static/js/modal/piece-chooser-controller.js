app.controller("pieceChooserController", function ($scope, data) {

    var side = data.side;
    $scope.availablePieces = ["queen", "rook", "bishop", "knight"];

    $scope.onPieceClicked = function(pieceType) {
        if ($scope.onSelect) {
            $scope.onSelect(pieceType);
        }
    };

    $scope.onCancel = function() {
        if ($scope.onSelect) {
            $scope.onSelect(null);
        }
    };

    $scope.getItemClass = function (pieceType) {
        var result = [];

        result.push("kek");
        result.push("piece");
        result.push(pieceType + "-" + side);

        return result;
    };

});