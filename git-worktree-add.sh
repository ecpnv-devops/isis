#!/usr/bin/env bash

BASENAME_0=$(basename $0)

usage() {
 echo ""                                                                                               >&2
 echo "$BASENAME_0 [options] branch_name"                                                                           >&2
 echo ""                                                                                               >&2
 echo "  -w whatif - don't run the command but do print it out.  Implies -v (verbose)"                 >&2
 echo "  -v verbose"                                                                                   >&2
 echo "  -d debug"                                                                                     >&2
 echo ""                                                                                               >&2
 echo "Examples:"                                                                                      >&2
 echo ""                                                                                               >&2
 echo "  sh $BASENAME_0    ESTUP2-123" >&2
 echo "  sh $BASENAME_0 -v ESTUP2-123" >&2
 echo "  sh $BASENAME_0 -w ESTUP2-123" >&2
 echo ""                                                                                               >&2
 echo ""                                                                                               >&2
}


WHATIF=false
VERBOSE=false
DEBUG=false
BRANCH=""


while getopts 'wvhd' opt
do
  case $opt in
    w) WHATIF=true
       VERBOSE=true
       ;;
    v) VERBOSE=true
      ;;
    d) DEBUG=true
      ;;
    h) usage
       exit 1
       ;;
    *) echo "unknown option $opt - aborting" >&2
       usage
       exit 1
      ;;
  esac
done

shift $((OPTIND-1))
BRANCH=$1

if [ "$BRANCH" = "" ]; then
  usage
  exit 1
fi


if [ "$VERBOSE" = "true" ]; then
  echo "BRANCH : $BRANCH"
  echo "WHATIF : $WHATIF"
fi

BRANCH_CHECK=$(git branch | grep "$BRANCH" | xargs )
if [ "$DEBUG" = "true" ]; then
  echo "BRANCH_CHECK : $BRANCH_CHECK"
fi

if [ "$BRANCH_CHECK" != "$BRANCH" ]; then
  echo "" >&2
  echo "branch '$BRANCH' does not exist, aborting" >&2
  echo "" >&2
  if [ "$DEBUG" = "true" ]; then
    echo "available branches are:" >&2
    git branch 
    echo "" >&2
  fi
  exit 1
fi

if [ -d "../$BRANCH" ]; then
  echo "" >&2
  echo "directory ../$BRANCH already exists, aborting" >&2
  echo "" >&2
  exit 1
fi

if [ "$VERBOSE" = "true" ]; then
  echo "git worktree add \"../$BRANCH\" \"$BRANCH\""
fi

if [ "$WHATIF" != "true" ]; then
  git worktree add "../$BRANCH" "$BRANCH"
fi

