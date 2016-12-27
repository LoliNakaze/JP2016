/**
 * Created by nakaze on 15/12/16.
 */
angular.module('test', [])
    .controller('controller', function ($scope, $http) {
        $scope.message = "";

        $scope.onPost = function onPost(keyEvent, channel, username, content) {
            console.log("send command");
            if (keyEvent.which === 13 && content.length > 0) {
                $scope.postMessage(channel, username, content);
                $scope.message = "";
            }
        };

        $scope.onClickGet = function onClickGet() {
            console.log("call");
            $http.get('http://localhost:8080/api/users')
                .then(function (response) {
                    $scope.users = response.data;
                    console.log("success");
                }, function (response) {
                    $scope.errorCode = response.status;
                    console.log("error");
                });
        };
        $scope.onClickPost = function onClickPost(username, password, avatar) {
            console.log("call");
            if (username == null || password == null || avatar == null) {
                alert("All the fields have to be filled.");
                return;
            }
            $http.put('http://localhost:8080/api/users/' + username + '/' + password + '/' + avatar)
                .then(function () {
                    $scope.users.push({username: username, avatar: avatar});
                    console.log("success");
                }, function (data, status) {
                    $scope.status = status;
                    console.log("error");
                });
        };
        $scope.loadChannels = function loadChannels() {
            console.log("call");
            $http.get('http://localhost:8080/api/main/channels')
                .then(function (response) {
                    $scope.channels = response.data;
                    console.log("channel list loaded successfully");
                }, function (response) {
                    $scope.errorCode = response.status;
                    console.log("error channels");
                });
        };
        $scope.createChannel = function createChannel(channel) {
            console.log("call channel creation");
            $http.put('http://localhost:8080/api/main/channels/' + channel)
                .then(function () {
                    $scope.channels.push({cname: channel});
                    console.log("success");
                }, function (data, status) {
                    $scope.status = status;
                    console.log("error");
                });
        };
        $scope.loadContent = function loadChannel(channel) {
            console.log("call load channel");
            $http.get('http://localhost:8080/api/main/channel/' + channel)
                .then(function (response) {
                    $scope.channel = response.data;
                    console.log("channel list loaded successfully");
                }, function (response) {
                    $scope.errorCode = response.status;
                    console.log("error channels");
                });
        };
        $scope.postMessage = function postMessage(channel, username, content) {
            console.log("call post message");
            $http.put('http://localhost:8080/api/main/channel/' + channel + '/' + username + '/' + content)
                .then(function () {
                    $scope.loadContent(channel);
                    console.log("success");
                }, function (response) {
                    $scope.errorCode = response.status;
                    console.log("error post message");
                });
        };
        $scope.selectChannel = function selectChannel(channel) {
            $scope.currentChannel = channel;
            $scope.loadContent(channel);
        };

        function load() {
            $scope.onClickGet();
            $scope.loadChannels();
            $scope.selectChannel("general");
            $scope.loadContent("general");
        }

        load();
    });