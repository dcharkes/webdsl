#!/bin/bash
cd succeed-web/
echo "{ nixpkgs ? ../../nixpkgs
, webdsl ? {outPath = builtins.storePath /nix/store/fy8d3qkfhk3k1y10jpdx09q4w43bnh0a-webdsl-9.7pre4063;}
, nixos ? ../../nixos
}:
let
  pkgs = import nixpkgs { system = \"i686-linux\"; };
  build = appname :
   with import \"\${nixos}/lib/testing.nix\" {system = \"i686-linux\";} ;
      runInMachineWithX {
        require = [ ./machine.nix ];
        drv = pkgs.stdenv.mkDerivation {
          name = \"webdsl-check\";
          buildInputs = [pkgs.apacheAntOpenJDK pkgs.oraclejdk pkgs.firefox15Pkgs.firefox webdsl];
          buildCommand = ''
            ensureDir \$out
            cp -R \${webdsl}/share/webdsl/webdsl-check/test/succeed-web/ succeed-web/
			
            cd succeed-web
            TOPDIR=\`pwd\`
            FAILED=\"\"
            export DISPLAY=:0.0
            header \"Running \${appname}\"
            result=\"\"
            cd \$TOPDIR/\`dirname ./\${appname}\`
            FILE=\`basename ./\${appname} .app\`

            echo \"Executing 'webdsl test-web \$FILE\"
            webdsl test-web \$FILE 2>&1 || export FAILED=\"1\"
            stopNest
            if test -z \"\$FAILED\"; then
              exit 0
            else
              exit 1
            fi
          '';
        };
      };
  
  list = [" > webtests.nix;
function containsElement () {
  for e in "${@:2}"; do [[ "$e" == "$1" ]] && return 0; done
  return 1
}
excludes=( 	"./manual/ajax-form-validation/templates/templates.app"
		"./manual/tutorial-splash/v1/data.app"
		"./manual/tutorial-splash/v1/rootpage.app"
		"./manual/tutorial-splash/v2/data.app"
		"./manual/tutorial-splash/v2/lib.app"
		"./manual/tutorial-splash/v2/rootpage.app"
		"./manual/tutorial-splash/v2/ui.app"
		"./manual/tutorial-splash/v3/data.app"
		"./manual/tutorial-splash/v3/invite.app"
		"./manual/tutorial-splash/v3/lib.app"
		"./manual/tutorial-splash/v3/rootpage.app"
		"./manual/tutorial-splash/v3/ui.app"
		"./manual/tutorial-splash/v4/ac.app"
		"./manual/tutorial-splash/v4/data.app"
		"./manual/tutorial-splash/v4/invite.app"
		"./manual/tutorial-splash/v4/lib.app"
		"./manual/tutorial-splash/v4/rootpage.app"
		"./manual/tutorial-splash/v4/ui.app")

 for app in $(find -name "*.app") 
do 
  if (!(containsElement "$app" ${excludes[@]})); then
    echo "	\"${app#\.\/}\"" >> webtests.nix;
  fi
done
echo "
  ];
  
  jobs = pkgs.lib.listToAttrs (map (f: pkgs.lib.nameValuePair (pkgs.lib.replaceChars [\"/\"] [\"_\"] f) (build f)) list);

in jobs" >> webtests.nix;