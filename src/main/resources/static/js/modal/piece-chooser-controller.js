app.controller("pieceChooserController", function ($scope, $uibModalInstance, data) {

    $scope.availablePieces = [PIECE_QUEEN, PIECE_ROOK, PIECE_BISHOP, PIECE_KNIGHT];

    $scope.onSuccess = function(pieceType) {
        $uibModalInstance.close(pieceType);
    };

    $scope.onCancel = function() {
        $uibModalInstance.dismiss();
    };

    $scope.getItemClass = function (pieceType) {
        var result = [];

        result.push("modal-piece");
        result.push(pieceType + "-" + data.side);

        return result;
    };

});