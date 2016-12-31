/**
 * Created by nakaze on 15/12/16.
 */

angular.module('index', [])
    .controller('controller', function ($scope, $http, $sce) {
        $scope.message = "";

        $scope.resetSignInForm = function () {
            $scope.username = "";
            $scope.password = "";
        };

        $scope.logChecker = function () {
            $http.get("https://localhost:8080/api/check/")
                .then(function (response) {
                    $scope.loggedUser = response.data;
                    $scope.isLogged = true;
                    $scope.notLogged = false;
                }, function (response) {
                    $scope.notLogged = true;
                    $scope.isLogged = false;
                    $scope.loggedUser = "";
                });
        };

        $scope.createEventBus = function () {
            $scope.eventBus = new EventBus("https://localhost:8080/eventbus/");

            $scope.eventBus.onopen = function () {
                $scope.eventBus.registerHandler("chat.to.client." + $scope.currentChannel, function (error, message) {
                    console.log("Logged to: chat.to.client." + $scope.currentChannel);
                    console.log('Received: ' + JSON.stringify(message));
                    var array = message.body.split('__::__', 4);
                    console.log(array[1] + ', ' + $scope.currentChannel);
                    $scope.original.push({date: array[0], username: array[2], content: array[3]});
                    console.log("updated with eventBus");
                });
                $scope.eventBus.registerHandler("log.to.client", function (error, message) {
                    var array = message.body.split(":");
                    if (array[0] == 'log') {
                        $scope.loggedUsers.push({username: array[1]});
                    }
                    else {
                        var x = $scope.loggedUsers.indexOf(array[1]);
                        if (x > -1) {
                            $scope.loggedUser[x] = null;
                            $scope.loggedUsers.splice(x, 1);
                        }
                    }
                })
            };

            $scope.eventBus.onclose = function () {
                $scope.eventBus.unregisterHandler("chat.to.client." + $scope.currentChannel, function () {
                    console.log("Logged out: " + "chat.to.client." + $scope.currentChannel);
                });
                $scope.eventBus.unregisterHandler("log.to.client", function () {
                    console.log("Logged out: " + "log.to.client");
                });
            }
        };

        $scope.selectChannel = function selectChannel(channel) {
            $scope.currentChannel = channel;
            if ($scope.eventBus != null)
                $scope.eventBus.close();
            $scope.createEventBus();
            $scope.loadContent(channel);
        };

        $scope.displayChannel = function selectChannel(channel) {
            if ($scope.currentChannel == channel)
                $scope.active = " >";
            else
                $scope.active = "";
        };

        $scope.getLoggedUsers = function getLoggedUsers() {
            console.log("call");
            $http.get('https://localhost:8080/api/logged/users/')
                .then(function (response) {
                    $scope.loggedUsers = response.data;
                    console.log("success");
                }, function (response) {
                    $scope.errorCode = response.status;
                    console.log("error");
                });
        };

        $scope.onPostMessage = function onPost(keyEvent, channel, username, content) {
            console.log("send command");
            if (keyEvent.which === 13 && content.length > 0) {
                $scope.postMessage(channel, username, content);
                $scope.message = "";
            }
        };

        $scope.onPostSearch = function onPost(keyEvent, content) {
            console.log("send command");
            if (keyEvent.which === 13 && content.length > 0) {
                $scope.searchChannel(content);
                $scope.message = "";
            }
            else {
                $scope.channel = $scope.original;
            }
        };

        $scope.authenticate = function authenticate(username, password) {
            console.log("call authenticate");
            $http.get('https://localhost:8080/api/authenticate/' + username + '/' + password + "/")
                .then(function (response) {
                    $scope.eventBus.publish("log.to.server", username);
                    alert("Successfully logged in !");
                    $scope.loggedUser = username;
                    $scope.isLogged = true;
                    $scope.notLogged = false;
                    $scope.resetSignInForm();
                    console.log("success");
                }, function (response) {
                    alert(response.statusText);
                    console.log("failure authenticate");
                })
        };

        $scope.logout = function logout(username) {
            console.log("call logout");
            $http.delete('https://localhost:8080/api/authenticate/' + username + '/')
                .then(function (response) {
                    $scope.eventBus.publish("unlog.to.server", username);
                    $scope.loggedUser = "";
                    $scope.isLogged = false;
                    $scope.notLogged = true;
                    alert(response.statusText);
                    console.log("success");
                }, function (response) {
                    alert(response.statusText);
                    console.log("failure authenticate");
                })
        };

        $scope.onClickGet = function onClickGet() {
            console.log("call");
            $http.get('https://localhost:8080/api/users/')
                .then(function (response) {
                    $scope.users = response.data;
                    console.log("success");
                }, function (response) {
                    $scope.errorCode = response.status;
                    console.log("error");
                });
        };

        $scope.searchChannel = function (content) {
            var tmp = [];
            for (i = 0; i < $scope.original.length ; i++) {
                if ($scope.original[i].content.indexOf(content) != -1) {
                    tmp.push($scope.original[i]);
                }
            }
            $scope.channel = tmp;
        };

        $scope.onClickPost = function onClickPost(username, password, avatar) {
            console.log("call");
            if (username == null || password == null || avatar == null) {
                alert("All the fields have to be filled.");
                return;
            }
            $http.put('https://localhost:8080/api/users/' + username + '/' + password + '/' + avatar + "/")
                .then(function () {
                    $scope.users.push({username: username, avatar: avatar});
                    $scope.resetSignInForm();
                    alert("You can log in with that account, now!");
                    console.log("success");
                }, function (data, status) {
                    $scope.status = status;
                    console.log("error");
                });
        };
        $scope.loadChannels = function loadChannels() {
            console.log("call");
            $http.get('https://localhost:8080/api/main/channels/')
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
            $http.put('https://localhost:8080/api/main/channels/' + channel + '/')
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
            $http.get('https://localhost:8080/api/main/channel/' + channel + '/')
                .then(function (response) {
                    $scope.original = response.data;
                    $scope.channel = response.data;
                    console.log("channel loaded successfully");
                }, function (response) {
                    $scope.errorCode = response.status;
                    console.log("error channels");
                });
        };
        $scope.postMessage = function postMessage(channel, username, content) {
            console.log("call post message");
            $http.put('https://localhost:8080/api/main/channel/' + channel + '/' + username + '/' + content + '/')
                .then(function () {
                    $scope.eventBus.publish("chat.to.server", channel + '__::__' + username + '__::__' + content);
                    console.log("success");
                }, function (response) {
                    $scope.errorCode = response.status;
                    console.log("error post message");
                });
        };

        function load() {
            $scope.logChecker();
            $scope.onClickGet();
            $scope.loadChannels();
            $scope.selectChannel("general");
            $scope.loadContent("general");
            $scope.getLoggedUsers();
        }

        load();
    });