app.controller("pieceChooserController", function ($scope, $uibModalInstance, data) {

    $scope.availablePieces = ["queen", "rook", "bishop", "knight"];

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