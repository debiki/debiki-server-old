/**
 * Copyright (C) 2014 Kaj Magnus Lindberg (born 1979)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/// <reference path="../../typedefs/angularjs/angular.d.ts" />
/// <reference path="../AdminApp.ts" />
/// <reference path="../RootScope.ts" />
/// <reference path="../QueryService.ts" />

//------------------------------------------------------------------------------
   module debiki2.admin.moderation {
//------------------------------------------------------------------------------

interface ModerationScope extends RootScope {
  recentPosts: Post[];
  /** Returns HTML that will be trusted by AngularJS, can be bound by ng-bind-html.*/
  textOrDiffFor(post: Post): any;
  approve(post: Post);
  reject(post: Post);
  delete(post: Post);
}


class ModerationController {

  public static $inject = ['$scope', '$sce', 'QueryService'];
  constructor(private $scope: ModerationScope, $sce: ng.ISCEService,
      private queryService: QueryService) {

    this.queryService.loadRecentPosts().then((posts: Post[]) => {
      this.$scope.recentPosts = posts;
    }).catch((reason) => {
      console.log('Error loading recent posts: '+ reason);
    });

    $scope.textOrDiffFor = (post: Post) => {
      return $sce.trustAsHtml(post.textOrDiffSafeHtml);
    }

    $scope.approve = (post: Post) => {
      doInlineAction(queryService.approvePost, post, 'Approved.');
    }

    $scope.reject = (post: Post) => {
      doInlineAction(queryService.rejectPost, post, 'Rejected.');
    }

    $scope.delete = (post: Post) => {
      doInlineAction(queryService.deletePost, post, 'Deleted.');
    }


    var doInlineAction = (queryServiceFn: (Post) => ng.IPromise<void>, post: Post,
        doneMessage: string) => {
      post.approveBtnText = '';
      post.hideRejectBtn = true;
      post.hideViewSuggsLink = true;
      post.inlineMessage = 'Wait...';
      queryServiceFn.call(queryService, post).then((data) => {
        post.inlineMessage = doneMessage;
      });
    };
  }

}


debiki2.admin.adminApp.controller('ModerationController', ModerationController);


//------------------------------------------------------------------------------
   }
//------------------------------------------------------------------------------
// vim: fdm=marker et ts=2 sw=2 tw=0 fo=tcqwn list
