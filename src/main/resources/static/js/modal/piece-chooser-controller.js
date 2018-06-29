app.controller("pieceChooserController", function ($scope, $uibModalInstance, data) {

    var side = data.side;
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
        result.push(pieceType + "-" + side);

        return result;
    };

});