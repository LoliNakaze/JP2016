/**
 * Created by nakaze on 15/12/16.
 */

angular.module('test', [])
    .controller('controller', function ($scope, $http) {
        $scope.message = "";
        $scope.eventBus = new EventBus("http://localhost:8080/eventbus/");

        $scope.eventBus.onopen = function() {
            $scope.eventBus.registerHandler("chat.to.client", function(error, message) {
                console.log('Received: ' + JSON.stringify(message));
                var array = message.body.split('__::__', 4);
                console.log(array[1] + ', ' + $scope.currentChannel);
                if (array[1] == $scope.currentChannel) {
                    console.log("updated with eventBus");
                    $scope.channel.push({date: array[0], username: array[2], content: array[3]});
                }
            })
        };

        $scope.onPost = function onPost(keyEvent, channel, username, content) {
            console.log("send command");
            if (keyEvent.which === 13 && content.length > 0) {
                $scope.postMessage(channel, username, content);
                $scope.message = "";
            }
        };

        $scope.authenticate = function authenticate(username, password) {
            console.log("call authenticate");
            $http.get('http://localhost:8080/api/authenticate/' + username + '/' + password)
                .then(function (response) {
                    alert("Successfully logged in !");
                    console.log("success");
                }, function (response) {
                    if (response.status == 402) {
                        alert("This user doesn't exist.");
                    } else if (response.status == 403) {
                        alert("Wrong password.");
                    } else {
                        alert("Please fill out all the * blanks");
                    }
                    console.log("failure authenticate");
                })
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
                    $scope.eventBus.publish("chat.to.server", channel + '__::__' + username + '__::__' + content);
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